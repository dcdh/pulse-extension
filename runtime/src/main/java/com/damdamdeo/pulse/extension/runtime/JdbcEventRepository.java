package com.damdamdeo.pulse.extension.runtime;

import com.damdamdeo.pulse.extension.core.*;
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
    public void save(final List<VersionizedEvent<K>> versionizedEvents) throws EventStoreException {
        Objects.requireNonNull(versionizedEvents);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             INSERT INTO T_EVENT (aggregaterootid, aggregateroottype, version, creationdate, eventtype, eventpayload) 
                             VALUES (?, ?, ?, ?, ?, to_json(?::json))
                             """)) {
            connection.setAutoCommit(false);
            for (VersionizedEvent<K> versionizedEvent : versionizedEvents) {
                preparedStatement.setString(1, versionizedEvent.event().id().id());
                preparedStatement.setString(2, getAggregateClass().getName());
                preparedStatement.setLong(3, versionizedEvent.version().version());
                preparedStatement.setTimestamp(4, Timestamp.from(instantProvider.now()));
                preparedStatement.setString(5, versionizedEvent.event().getClass().getName());
                preparedStatement.setString(6, objectMapper.writeValueAsString(versionizedEvent.event()));
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
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
                             SELECT COUNT(*) AS nbOfEvents FROM T_EVENT e WHERE e.aggregaterootid = ? AND e.aggregateroottype = ?
                             """);
             final PreparedStatement loadStmt = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT * FROM T_EVENT e WHERE e.aggregaterootid = ? AND e.aggregateroottype = ? ORDER BY e.version ASC
                             """)) {
            connection.setAutoCommit(false);
            nbOfEventsStmt.setString(1, id.id());
            nbOfEventsStmt.setString(2, getAggregateClass().getName());
            try (final ResultSet nbOfEventsResultSet = nbOfEventsStmt.executeQuery()) {
                nbOfEventsResultSet.next();
                final int nbOfEvents = nbOfEventsResultSet.getInt("nbOfEvents");
                loadStmt.setString(1, id.id());
                loadStmt.setString(2, getAggregateClass().getName());
                final List<Event<K>> events = new ArrayList<>(nbOfEvents);
                try (final ResultSet resultSet = loadStmt.executeQuery()) {
                    while (resultSet.next()) {
                        events.add((Event<K>)
                                objectMapper.readValue(resultSet.getString("eventpayload"),
                                        Class.forName(resultSet.getString("eventtype"))));
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

    // FCK tester bordel de merde !
    @Override
    public Optional<AggregateVersion> getLastVersionByAggregateId(final AggregateId aggregateId) throws EventStoreException {
        Objects.requireNonNull(aggregateId);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT version FROM T_EVENT e WHERE e.aggregaterootid = ? AND e.aggregateroottype = ?
                             ORDER BY version DESC LIMIT 1
                             """)) {
            preparedStatement.setString(1, aggregateId.id());
            preparedStatement.setString(2, getAggregateClass().getName());
            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new AggregateVersion(
                            resultSet.getInt("version")));
                } else {
                    return Optional.empty();
                }
            }
        } catch (final SQLException e) {
            throw new EventStoreException(e);
        }
    }

    abstract protected Class<A> getAggregateClass();
}
