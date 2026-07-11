package com.damdamdeo.pulse.extension.writer.runtime.serializer;

import com.damdamdeo.pulse.extension.common.runtime.serialization.BusinessMapper;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.damdamdeo.pulse.extension.core.encryption.UnableToProvidePassphraseException;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class EventTestRepository {

    @Inject
    DataSource dataSource;

    @Inject
    PassphraseProvider passphraseProvider;

    @Inject
    @BusinessMapper
    ObjectMapper objectMapper;

    public void insert(final Event<?> event, final AggregateRoot<?> aggregateRoot, final OwnedBy ownedBy, final ExecutedBy executedBy) {
        insert(List.of(event), aggregateRoot, ownedBy, executedBy);
    }

    public void insert(final List<Event<?>> events, final AggregateRoot<?> aggregateRoot, final OwnedBy ownedBy, final ExecutedBy executedBy) {
        try (final Connection connection = dataSource.getConnection()) {
            final Passphrase provided = passphraseProvider.provide(ownedBy);
            int version = 0;
            for (final Event<?> event : events) {
                final String eventPayload = objectMapper.writeValueAsString(event);
                try (final PreparedStatement preparedStatement = connection.prepareStatement(
                        // language=sql
                        """
                                INSERT INTO event (aggregate_root_id, aggregate_root_type, version, stored_at, event_type, event_payload, owned_by, belongs_to, executed_by) 
                                VALUES (?, ?, ?, ?, ?, public.pgp_sym_encrypt(?::text, ?), ?, ?, ?)
                                """)) {
                    preparedStatement.setString(1, aggregateRoot.id().id());
                    preparedStatement.setString(2, aggregateRoot.getClass().getSimpleName());
                    preparedStatement.setLong(3, version);
                    preparedStatement.setTimestamp(4, Timestamp.from(Instant.now()));
                    preparedStatement.setString(5, event.getClass().getSimpleName());
                    preparedStatement.setString(6, eventPayload);
                    preparedStatement.setString(7, new String(provided.passphrase()));
                    preparedStatement.setString(8, ownedBy.id());
                    preparedStatement.setString(9, aggregateRoot.belongsTo().id());
                    preparedStatement.setString(10, executedBy.value());
                    preparedStatement.executeUpdate();
                } catch (final SQLException e) {
                    throw new RuntimeException(e);
                }
                version++;
            }
            final String aggregateRootPayload = objectMapper.writeValueAsString(aggregateRoot);
            try (final PreparedStatement preparedStatement = connection.prepareStatement(
                    // language=sql
                    """
                            INSERT INTO aggregate_root (aggregate_root_id, aggregate_root_type, last_version, aggregate_root_payload, owned_by, belongs_to)
                            VALUES (?,?,?, public.pgp_sym_encrypt(?::text, ?), ?, ?)
                            """)) {
                preparedStatement.setString(1, aggregateRoot.id().id());
                preparedStatement.setString(2, aggregateRoot.getClass().getSimpleName());
                preparedStatement.setLong(3, version);
                preparedStatement.setString(4, aggregateRootPayload);
                preparedStatement.setString(5, new String(provided.passphrase()));
                preparedStatement.setString(6, ownedBy.id());
                preparedStatement.setString(7, aggregateRoot.belongsTo().id());
                preparedStatement.executeUpdate();
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (UnableToProvidePassphraseException | SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
