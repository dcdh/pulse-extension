package com.damdamdeo.pulse.extension.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.VersionizedAggregateRoot;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.event.EventStoreException;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class JdbcEventRepository<A extends AggregateRoot<K>, K extends AggregateId> implements EventRepository<A, K> {

    @Inject
    DataSource dataSource;

    @Inject
    InstantProvider instantProvider;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void save(final List<VersionizedEvent<K>> versionizedEvents, final AggregateRoot<K> aggregateRoot) throws EventStoreException {
        Objects.requireNonNull(versionizedEvents);
        Objects.requireNonNull(aggregateRoot);
        if (versionizedEvents.isEmpty()) {
            return;
        }
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement eventPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             INSERT INTO t_event (aggregate_root_id, aggregate_root_type, version, creation_date, event_type, event_payload) 
                             VALUES (?, ?, ?, ?, ?, to_json(?::json))
                             """);
             final PreparedStatement aggregatePreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             INSERT INTO t_aggregate_root (aggregate_root_id, aggregate_root_type, last_version, aggregate_root_payload)
                             VALUES (?,?,?, to_json(?::json))
                             ON CONFLICT (aggregate_root_id, aggregate_root_type)
                             DO UPDATE
                             SET
                                 last_version = EXCLUDED.last_version,
                                 aggregate_root_payload = EXCLUDED.aggregate_root_payload;
                             """)) {
            connection.setAutoCommit(false);
            K aggregateId = versionizedEvents.getFirst().event().id();
            AggregateVersion lastVersion = null;
            for (VersionizedEvent<K> versionizedEvent : versionizedEvents) {
                eventPreparedStatement.setString(1, versionizedEvent.event().id().id());
                eventPreparedStatement.setString(2, getAggregateClass().getName());
                eventPreparedStatement.setLong(3, versionizedEvent.version().version());
                eventPreparedStatement.setTimestamp(4, Timestamp.from(instantProvider.now()));
                eventPreparedStatement.setString(5, versionizedEvent.event().getClass().getName());
                eventPreparedStatement.setString(6, objectMapper.writeValueAsString(versionizedEvent.event()));
                eventPreparedStatement.addBatch();
                lastVersion = versionizedEvent.version();
            }
            eventPreparedStatement.executeBatch();
            aggregatePreparedStatement.setString(1, aggregateId.id());
            aggregatePreparedStatement.setString(2, getAggregateClass().getName());
            aggregatePreparedStatement.setLong(3, lastVersion.version());
            aggregatePreparedStatement.setString(4, objectMapper.writeValueAsString(aggregateRoot));
            aggregatePreparedStatement.executeUpdate();
        } catch (final JsonProcessingException | SQLException e) {
            throw new EventStoreException(e);
        }
    }

    @Override
    public List<Event<K>> loadOrderByVersionASC(K id) throws EventStoreException {
        Objects.requireNonNull(id);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement nbOfEventsStmt = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT COUNT(*) AS nb_of_events FROM t_event e WHERE e.aggregate_root_id = ? AND e.aggregate_root_type = ?
                             """);
             final PreparedStatement loadStmt = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT * FROM t_event e WHERE e.aggregate_root_id = ? AND e.aggregate_root_type = ? ORDER BY e.version ASC
                             """)) {
            connection.setAutoCommit(false);
            nbOfEventsStmt.setString(1, id.id());
            nbOfEventsStmt.setString(2, getAggregateClass().getName());
            try (final ResultSet nbOfEventsResultSet = nbOfEventsStmt.executeQuery()) {
                nbOfEventsResultSet.next();
                final int nbOfEvents = nbOfEventsResultSet.getInt("nb_of_events");
                loadStmt.setString(1, id.id());
                loadStmt.setString(2, getAggregateClass().getName());
                final List<Event<K>> events = new ArrayList<>(nbOfEvents);
                try (final ResultSet resultSet = loadStmt.executeQuery()) {
                    while (resultSet.next()) {
                        events.add((Event<K>)
                                objectMapper.readValue(resultSet.getString("event_payload"),
                                        Class.forName(resultSet.getString("event_type"))));
                    }
                } catch (ClassNotFoundException | JsonProcessingException e) {
                    throw new EventStoreException(e);
                }
                return events;
            }
        } catch (final SQLException e) {
            throw new EventStoreException(e);
        }
    }

    @Override
    public List<Event<K>> loadOrderByVersionASC(final K id, final AggregateVersion aggregateVersionRequested) throws EventStoreException {
        Objects.requireNonNull(id);
        Objects.requireNonNull(aggregateVersionRequested);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement loadStmt = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT * FROM t_event e WHERE e.aggregate_root_id = ? AND e.aggregate_root_type = ? AND e.version <= ? ORDER BY e.version ASC
                             """)) {
            connection.setAutoCommit(false);
            loadStmt.setString(1, id.id());
            loadStmt.setString(2, getAggregateClass().getName());
            loadStmt.setInt(3, aggregateVersionRequested.version());
            final List<Event<K>> events = new ArrayList<>(aggregateVersionRequested.version());
            try (final ResultSet resultSet = loadStmt.executeQuery()) {
                while (resultSet.next()) {
                    events.add((Event<K>)
                            objectMapper.readValue(resultSet.getString("event_payload"),
                                    Class.forName(resultSet.getString("event_type"))));
                }
            } catch (ClassNotFoundException | JsonProcessingException e) {
                throw new EventStoreException(e);
            }
            return events;
        } catch (final SQLException e) {
            throw new EventStoreException(e);
        }
    }

    @Override
    public Optional<VersionizedAggregateRoot<A>> findLastVersionById(final K id) {
        Objects.requireNonNull(id);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement aggregateRootStmt = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT * FROM t_aggregate_root e WHERE e.aggregate_root_id = ? AND e.aggregate_root_type = ? ORDER BY e.last_version DESC
                             """)) {
            connection.setAutoCommit(false);
            aggregateRootStmt.setString(1, id.id());
            aggregateRootStmt.setString(2, getAggregateClass().getName());
            try (final ResultSet aggregateRootStmtResultSet = aggregateRootStmt.executeQuery()) {
                if (aggregateRootStmtResultSet.next()) {
                    final A aggregateRoot = objectMapper.readValue(aggregateRootStmtResultSet.getString("aggregate_root_payload"),
                            getAggregateClass());
                    return Optional.of(new VersionizedAggregateRoot<>(aggregateRoot,
                            new AggregateVersion(
                                    aggregateRootStmtResultSet.getInt("last_version"))));
                } else {
                    return Optional.empty();
                }
            } catch (JsonProcessingException e) {
                throw new EventStoreException(e);
            }
        } catch (final SQLException e) {
            throw new EventStoreException(e);
        }
    }

    abstract protected Class<A> getAggregateClass();
}
