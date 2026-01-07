package com.damdamdeo.pulse.extension.writer.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.VersionizedAggregateRoot;
import com.damdamdeo.pulse.extension.core.encryption.DecryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.OwnedByExecutedByEncoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public abstract class JdbcPostgresEventRepository<A extends AggregateRoot<K>, K extends AggregateId> implements EventRepository<A, K> {

    final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    @Inject
    Provider<DataSource> dataSource;

    @Inject
    InstantProvider instantProvider;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    PassphraseProvider passphraseProvider;

    @Inject
    DecryptionService decryptionService;

    @Inject
    EventClazzDiscovery eventClazzDiscovery;

    @Inject
    OwnedByExecutedByEncoder ownedByExecutedByEncoder;

    @Override
    public void save(final List<VersionizedEvent> versionizedEvents, final AggregateRoot<K> aggregateRoot, final ExecutedBy executedBy)
            throws EventStoreException {
        Objects.requireNonNull(versionizedEvents);
        Objects.requireNonNull(aggregateRoot);
        Objects.requireNonNull(executedBy);
        if (versionizedEvents.isEmpty()) {
            return;
        }
        try (final Connection connection = dataSource.get().getConnection();
             final PreparedStatement eventPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             INSERT INTO event (aggregate_root_id, aggregate_root_type, version, creation_date, event_type, event_payload, owned_by, belongs_to, executed_by) 
                             VALUES (?, ?, ?, ?, ?, public.pgp_sym_encrypt(?::text, ?), ?, ?, ?)
                             """);
             final PreparedStatement aggregatePreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             INSERT INTO aggregate_root (aggregate_root_id, aggregate_root_type, last_version, aggregate_root_payload, owned_by, belongs_to)
                             VALUES (?,?,?, public.pgp_sym_encrypt(?::text, ?), ?, ?)
                             ON CONFLICT (aggregate_root_id, aggregate_root_type)
                             DO UPDATE
                             SET
                                 last_version = EXCLUDED.last_version,
                                 aggregate_root_payload = EXCLUDED.aggregate_root_payload;
                             """)) {
            connection.setAutoCommit(false);
            final K aggregateId = aggregateRoot.id();
            final OwnedBy ownedBy = aggregateRoot.ownedBy();
            AggregateVersion lastVersion = null;
            for (final VersionizedEvent versionizedEvent : versionizedEvents) {
                final String eventPayload = objectMapper.writeValueAsString(versionizedEvent.event());
                LOGGER.fine(eventPayload);
                eventPreparedStatement.setString(1, aggregateId.id());
                eventPreparedStatement.setString(2, getAggregateClass().getSimpleName());
                eventPreparedStatement.setLong(3, versionizedEvent.version().version());
                eventPreparedStatement.setTimestamp(4, Timestamp.from(instantProvider.now()));
                eventPreparedStatement.setString(5, versionizedEvent.event().getClass().getSimpleName());
                eventPreparedStatement.setString(6, eventPayload);
                eventPreparedStatement.setString(7, new String(passphraseProvider.provide(ownedBy).passphrase()));
                eventPreparedStatement.setString(8, ownedBy.id());
                eventPreparedStatement.setString(9, aggregateRoot.belongsTo().aggregateId().id());
                eventPreparedStatement.setString(10, executedBy.encode(ownedByExecutedByEncoder.executedByEncoder(ownedBy)));
                eventPreparedStatement.addBatch();
                lastVersion = versionizedEvent.version();
            }
            eventPreparedStatement.executeBatch();
            final String aggregateRootPayload = objectMapper.writeValueAsString(aggregateRoot);
            LOGGER.fine(aggregateRootPayload);
            aggregatePreparedStatement.setString(1, aggregateId.id());
            aggregatePreparedStatement.setString(2, getAggregateClass().getSimpleName());
            aggregatePreparedStatement.setLong(3, lastVersion.version());
            aggregatePreparedStatement.setString(4, aggregateRootPayload);
            aggregatePreparedStatement.setString(5, new String(passphraseProvider.provide(ownedBy).passphrase()));
            aggregatePreparedStatement.setString(6, ownedBy.id());
            aggregatePreparedStatement.setString(7, aggregateRoot.belongsTo().aggregateId().id());
            aggregatePreparedStatement.executeUpdate();
        } catch (final JsonProcessingException | SQLException e) {
            throw new EventStoreException(e);
        }
    }

    @Override
    public List<Event> loadOrderByVersionASC(K id) throws EventStoreException {
        Objects.requireNonNull(id);
        try (final Connection connection = dataSource.get().getConnection();
             final PreparedStatement nbOfEventsStmt = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT COUNT(*) AS nb_of_events FROM event e WHERE e.aggregate_root_id = ? AND e.aggregate_root_type = ?
                             """);
             final PreparedStatement loadStmt = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT e.event_payload AS event_payload, e.event_type AS event_type, e.owned_by AS owned_by FROM event e WHERE e.aggregate_root_id = ? AND e.aggregate_root_type = ? ORDER BY e.version ASC
                             """)) {
            connection.setAutoCommit(false);
            nbOfEventsStmt.setString(1, id.id());
            nbOfEventsStmt.setString(2, getAggregateClass().getSimpleName());
            try (final ResultSet nbOfEventsResultSet = nbOfEventsStmt.executeQuery()) {
                nbOfEventsResultSet.next();
                final int nbOfEvents = nbOfEventsResultSet.getInt("nb_of_events");
                loadStmt.setString(1, id.id());
                loadStmt.setString(2, getAggregateClass().getSimpleName());
                final List<Event> events = new ArrayList<>(nbOfEvents);
                try (final ResultSet resultSet = loadStmt.executeQuery()) {
                    while (resultSet.next()) {
                        final OwnedBy ownedBy = new OwnedBy(resultSet.getString("owned_by"));
                        final DecryptedPayload decryptedEventPayload = decryptionService.decrypt(new EncryptedPayload(resultSet.getBytes("event_payload")), ownedBy);
                        LOGGER.fine(new String(decryptedEventPayload.payload()));
                        events.add((Event)
                                objectMapper.readValue(decryptedEventPayload.payload(),
                                        eventClazzDiscovery.from(resultSet.getString("event_type"))));
                    }
                } catch (ClassNotFoundException | IOException e) {
                    throw new EventStoreException(e);
                }
                return events;
            }
        } catch (final SQLException e) {
            throw new EventStoreException(e);
        }
    }

    @Override
    public List<Event> loadOrderByVersionASC(final K id, final AggregateVersion aggregateVersionRequested) throws EventStoreException {
        Objects.requireNonNull(id);
        Objects.requireNonNull(aggregateVersionRequested);
        try (final Connection connection = dataSource.get().getConnection();
             final PreparedStatement loadStmt = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT e.event_payload AS event_payload, e.event_type AS event_type, e.owned_by AS owned_by FROM event e WHERE e.aggregate_root_id = ? AND e.aggregate_root_type = ? AND e.version <= ? ORDER BY e.version ASC
                             """)) {
            connection.setAutoCommit(false);
            loadStmt.setString(1, id.id());
            loadStmt.setString(2, getAggregateClass().getSimpleName());
            loadStmt.setInt(3, aggregateVersionRequested.version());
            final List<Event> events = new ArrayList<>(aggregateVersionRequested.version());
            try (final ResultSet resultSet = loadStmt.executeQuery()) {
                while (resultSet.next()) {
                    final OwnedBy ownedBy = new OwnedBy(resultSet.getString("owned_by"));
                    final DecryptedPayload decryptedEventPayload = decryptionService.decrypt(new EncryptedPayload(resultSet.getBytes("event_payload")), ownedBy);
                    LOGGER.fine(new String(decryptedEventPayload.payload()));
                    events.add((Event)
                            objectMapper.readValue(decryptedEventPayload.payload(),
                                    eventClazzDiscovery.from(resultSet.getString("event_type"))));
                }
            } catch (ClassNotFoundException | IOException e) {
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
        try (final Connection connection = dataSource.get().getConnection();
             final PreparedStatement aggregateRootStmt = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT e.last_version AS last_version, e.aggregate_root_payload AS aggregate_root_payload, e.owned_by AS owned_by FROM aggregate_root e WHERE e.aggregate_root_id = ? AND e.aggregate_root_type = ? ORDER BY e.last_version DESC
                             """)) {
            connection.setAutoCommit(false);
            aggregateRootStmt.setString(1, id.id());
            aggregateRootStmt.setString(2, getAggregateClass().getSimpleName());
            try (final ResultSet aggregateRootStmtResultSet = aggregateRootStmt.executeQuery()) {
                if (aggregateRootStmtResultSet.next()) {
                    final OwnedBy ownedBy = new OwnedBy(aggregateRootStmtResultSet.getString("owned_by"));
                    final DecryptedPayload decryptedEventPayload = decryptionService.decrypt(new EncryptedPayload(aggregateRootStmtResultSet.getBytes("aggregate_root_payload")), ownedBy);
                    LOGGER.fine(new String(decryptedEventPayload.payload()));
                    final A aggregateRoot = objectMapper.readValue(decryptedEventPayload.payload(), getAggregateClass());
                    return Optional.of(new VersionizedAggregateRoot<>(aggregateRoot,
                            new AggregateVersion(
                                    aggregateRootStmtResultSet.getInt("last_version"))));
                } else {
                    return Optional.empty();
                }
            } catch (IOException e) {
                throw new EventStoreException(e);
            }
        } catch (final SQLException e) {
            throw new EventStoreException(e);
        }
    }

    abstract protected Class<A> getAggregateClass();
}
