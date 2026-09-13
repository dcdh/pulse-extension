package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.TestUsernameEncoder;
import com.damdamdeo.pulse.extension.core.executedby.UnableToEncodeException;
import com.damdamdeo.pulse.extension.core.traceability.OwnedByProviderException;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresOwnedByProvider;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JdbcPostgresOwnedByProviderTest {

    private static ExecutedBy BOB = new ExecutedBy.EndUser(new Username("bob@mail.com"));

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresOwnedByProvider jdbcPostgresOwnedByProvider;

    @Inject
    DataSource dataSource;

    @BeforeAll
    void prepareDatasource() {
        // language=sql
        final String table = """
                CREATE TABLE IF NOT EXISTS todo_taking.event (
                  aggregate_root_type character varying(255) not null,
                  aggregate_root_id character varying(255) not null,
                  version bigint not null,
                  stored_at timestamptz not null,
                  event_type character varying(255) not null,
                  event_payload bytea not null CHECK (octet_length(event_payload) <= 1000 * 1024),
                  owned_by character varying(255) not null,
                  belongs_to character varying(255) not null,
                  executed_by character varying(255) not null,
                  CONSTRAINT event_pkey PRIMARY KEY (aggregate_root_id, aggregate_root_type, version),
                  CONSTRAINT event_unique UNIQUE (aggregate_root_id, aggregate_root_type, version),
                  CONSTRAINT executed_by_format_chk CHECK (executed_by = 'A' OR executed_by LIKE 'EU:%' OR executed_by LIKE 'SA:%' OR executed_by = 'NA')
                );
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(table)) {
            preparedStatement.executeUpdate();
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    void shouldProvideOwnedByFromAggregateId() throws SQLException, OwnedByProviderException {
        // Given
        insertEvent("00000000-0000-0000-0000-000000000001", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                Todo.OWNED_BY_USER_1, BOB);

        // When
        final OwnedBy provided = jdbcPostgresOwnedByProvider.provide(new AnyAggregateId("00000000-0000-0000-0000-000000000001"));

        // Then
        assertThat(provided).isEqualTo(Todo.OWNED_BY_USER_1);
    }

    @Test
    void shouldFailFastWhenOwnedByIsUnknown() {
        // Given

        // When / Then
        assertThatThrownBy(() -> jdbcPostgresOwnedByProvider.provide(new AnyAggregateId("00000000-0000-0000-0000-000000000001")))
                .isExactlyInstanceOf(OwnedByProviderException.class)
                .cause()
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Should not happen, the event should have been persisted before.");
    }

// TODO maybe later
//    @Test
//    void shouldThrowExceptionOnSQLException() {
//        // Given
//
//        // When / Then
//        assertThatThrownBy(() -> {
//            //
//            try (final Connection connection = dataSource.getConnection()) {
//                connection.setAutoCommit(false);
//                connection.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
//                jdbcPostgresOwnedByProvider.provide(new AnyAggregateId("00000000-0000-0000-0000-000000000001"));
//            } catch (final SQLException e) {
//                // do nothing
//            }
//            throw new IllegalStateException("Should not reach this point");
//        }).isExactlyInstanceOf(RuntimeException.class);
//    }

    private void insertEvent(final String aggregateRootId, final String aggregateRootType, final Integer version,
                             final Instant storedAt, final String eventType, final String encryptedEventPayload,
                             final OwnedBy ownedBy, final ExecutedBy executedBy) throws SQLException {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             INSERT INTO event (aggregate_root_id, aggregate_root_type, version, stored_at, event_type, event_payload, owned_by, belongs_to, executed_by) 
                             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                             """)) {
            preparedStatement.setString(1, aggregateRootId);
            preparedStatement.setString(2, aggregateRootType);
            preparedStatement.setLong(3, version);
            preparedStatement.setTimestamp(4, Timestamp.from(storedAt));
            preparedStatement.setString(5, eventType);
            preparedStatement.setBytes(6, encryptedEventPayload.getBytes(StandardCharsets.UTF_8));
            preparedStatement.setString(7, ownedBy.id());
            preparedStatement.setString(8, aggregateRootId);
            preparedStatement.setString(9, executedBy.encode(TestUsernameEncoder.INSTANCE, ownedBy).encoded());
            preparedStatement.executeUpdate();
        } catch (final UnableToEncodeException e) {
            throw new RuntimeException(e);
        }
    }
}
