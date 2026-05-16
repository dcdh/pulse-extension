package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoder;
import com.damdamdeo.pulse.extension.core.executedby.OwnedByExecutedByEncoder;
import com.damdamdeo.pulse.extension.core.executedby.TestExecutedByEncoder;
import com.damdamdeo.pulse.extension.writer.runtime.InstantProvider;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.postgresql.util.PSQLException;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcPostgresEventRepositoryTest {

    private static ExecutedBy BOB = new ExecutedBy.EndUser("bob", true);

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .overrideConfigKey("quarkus.devservices.enabled", "true")
            .overrideRuntimeConfigKey("pulse.datasource.init-at-startup", "true")
            .withConfigurationResource("application.properties");

    @Inject
    EventRepository<Todo, TodoId> todoEventRepository;

    @Inject
    DataSource dataSource;

    @ApplicationScoped
    static class StubOwnedByExecutedByEncoder implements OwnedByExecutedByEncoder {

        @Override
        public ExecutedByEncoder executedByEncoder(final OwnedBy ownedBy) {
            return TestExecutedByEncoder.INSTANCE;
        }
    }

    @ApplicationScoped
    static class StubInstantProvider implements InstantProvider {

        @Override
        public Instant now() {
            return Instant.parse("2025-10-13T18:00:00Z");
        }
    }

    @ApplicationScoped
    static class StubPassphraseProvider implements PassphraseProvider {

        @Override
        public Passphrase provide(final OwnedBy ownedBy) {
            return PassphraseSample.PASSPHRASE;
        }
    }

    @ApplicationScoped
    static class StubPassphraseRepository implements PassphraseRepository {

        @Override
        public Optional<Passphrase> retrieve(final OwnedBy ownedBy) {
            return Optional.of(PassphraseSample.PASSPHRASE);
        }

        @Override
        public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
            throw new IllegalStateException("Should not be called");
        }
    }

    @Test
    @Order(1)
    void shouldSave() {
        // Given
        final List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), BOB)));

        // When
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        new TodoId("Damien", TodoId.SEQUENCE_NUMBER_1),
                        "lorem ipsum",
                        Status.IN_PROGRESS,
                        false
                ), BOB);

        // Then
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement tEventPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT aggregate_root_id, aggregate_root_type, version, stored_at, event_type, event_payload, owned_by, belongs_to, executed_by
                                 FROM event WHERE aggregate_root_id = ? AND aggregate_root_type = ?
                             """);
             final PreparedStatement tAggregateRootPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT aggregate_root_id, aggregate_root_type, last_version, aggregate_root_payload, owned_by, belongs_to
                                 FROM aggregate_root WHERE aggregate_root_id = ? AND aggregate_root_type = ?
                             """)) {
            tEventPreparedStatement.setString(1, "Damien-000001");
            tEventPreparedStatement.setString(2, "Todo");
            tAggregateRootPreparedStatement.setString(1, "Damien-000001");
            tAggregateRootPreparedStatement.setString(2, "Todo");
            try (final ResultSet tEventResultSet = tEventPreparedStatement.executeQuery();
                 final ResultSet tAggregateRootResultSet = tAggregateRootPreparedStatement.executeQuery()) {
                tEventResultSet.next();
                tAggregateRootResultSet.next();
                assertAll(
                        () -> assertThat(tEventResultSet.getString("aggregate_root_id")).isEqualTo("Damien-000001"),
                        () -> assertThat(tEventResultSet.getString("aggregate_root_type")).isEqualTo("Todo"),
                        () -> assertThat(tEventResultSet.getLong("version")).isEqualTo(0),
                        () -> assertThat(tEventResultSet.getString("stored_at")).isEqualTo("2025-10-13 20:00:00+02"),
                        () -> assertThat(tEventResultSet.getString("event_type")).isEqualTo("NewTodoCreated"),
                        () -> assertThat(tEventResultSet.getString("event_payload")).startsWith("\\x"),
                        () -> assertThat(tEventResultSet.getString("owned_by")).isEqualTo("Damien"),
                        () -> assertThat(tEventResultSet.getString("belongs_to")).isEqualTo("Damien-000001"),
                        () -> assertThat(tEventResultSet.getString("executed_by")).isEqualTo("EU:encodedbob"),
                        () -> assertThat(tAggregateRootResultSet.getString("aggregate_root_id")).isEqualTo(
                                "Damien-000001"),
                        () -> assertThat(tAggregateRootResultSet.getString(
                                "aggregate_root_type")).isEqualTo("Todo"),
                        () -> assertThat(tAggregateRootResultSet.getLong(
                                "last_version")).isEqualTo
                                (0),
                        () -> assertThat(tAggregateRootResultSet.getString("aggregate_root_payload")).startsWith("\\x"),
                        () -> assertThat(tAggregateRootResultSet.getString("owned_by")).isEqualTo("Damien"),
                        () -> assertThat(tAggregateRootResultSet.getString("belongs_to")).isEqualTo("Damien-000001"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(2)
    void shouldUpsertAggregateRoot() {
        // Given
        todoEventRepository.save(List.of(
                        new VersionizedEvent<>(new AggregateVersion(0),
                                new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), BOB))),
                new Todo(
                        new TodoId("Damien", TodoId.SEQUENCE_NUMBER_2),
                        "lorem ipsum",
                        Status.IN_PROGRESS,
                        false
                ), BOB);

        // When
        todoEventRepository.save(List.of(
                        new VersionizedEvent<>(new AggregateVersion(1),
                                new ExecutedByEvent<>(new TodoMarkedAsDone(), BOB))),
                new Todo(
                        new TodoId("Damien", TodoId.SEQUENCE_NUMBER_2),
                        "lorem ipsum",
                        Status.DONE,
                        false
                ), BOB);

        // Then
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement tEventPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT aggregate_root_id, aggregate_root_type, version, stored_at, event_type, event_payload, owned_by, belongs_to, executed_by
                                 FROM event WHERE aggregate_root_id = ? AND aggregate_root_type = ?
                             """);
             final PreparedStatement tAggregateRootPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT aggregate_root_id, aggregate_root_type, last_version, aggregate_root_payload, owned_by, belongs_to
                                 FROM aggregate_root WHERE aggregate_root_id = ? AND aggregate_root_type = ?
                             """)) {
            tEventPreparedStatement.setString(1, "Damien-000002");
            tEventPreparedStatement.setString(2, "Todo");
            tAggregateRootPreparedStatement.setString(1, "Damien-000002");
            tAggregateRootPreparedStatement.setString(2, "Todo");
            try (final ResultSet tEventResultSet = tEventPreparedStatement.executeQuery();
                 final ResultSet tAggregateRootResultSet = tAggregateRootPreparedStatement.executeQuery()) {
                tEventResultSet.next();
                assertAll(
                        () -> assertThat(tEventResultSet.getString("aggregate_root_id")).isEqualTo("Damien-000002"),
                        () -> assertThat(tEventResultSet.getString("aggregate_root_type")).isEqualTo("Todo"),
                        () -> assertThat(tEventResultSet.getLong("version")).isEqualTo(0),
                        () -> assertThat(tEventResultSet.getString("stored_at")).isEqualTo("2025-10-13 20:00:00+02"),
                        () -> assertThat(tEventResultSet.getString("event_type")).isEqualTo("NewTodoCreated"),
                        () -> assertThat(tEventResultSet.getString("event_payload")).startsWith("\\x"),
                        () -> assertThat(tEventResultSet.getString("owned_by")).isEqualTo("Damien"),
                        () -> assertThat(tEventResultSet.getString("belongs_to")).isEqualTo("Damien-000002"),
                        () -> assertThat(tEventResultSet.getString("executed_by")).isEqualTo("EU:encodedbob"));
                tEventResultSet.next();
                assertAll(
                        () -> assertThat(tEventResultSet.getString("aggregate_root_id")).isEqualTo("Damien-000002"),
                        () -> assertThat(tEventResultSet.getString("aggregate_root_type")).isEqualTo("Todo"),
                        () -> assertThat(tEventResultSet.getLong("version")).isEqualTo(1),
                        () -> assertThat(tEventResultSet.getString("stored_at")).isEqualTo("2025-10-13 20:00:00+02"),
                        () -> assertThat(tEventResultSet.getString("event_type")).isEqualTo("TodoMarkedAsDone"),
                        () -> assertThat(tEventResultSet.getString("event_payload")).startsWith("\\x"),
                        () -> assertThat(tEventResultSet.getString("owned_by")).isEqualTo("Damien"),
                        () -> assertThat(tEventResultSet.getString("belongs_to")).isEqualTo("Damien-000002"),
                        () -> assertThat(tEventResultSet.getString("executed_by")).isEqualTo("EU:encodedbob"));
                tAggregateRootResultSet.next();
                assertAll(
                        () -> assertThat(tAggregateRootResultSet.getString("aggregate_root_id")).isEqualTo(
                                "Damien-000002"),
                        () -> assertThat(tAggregateRootResultSet.getString(
                                "aggregate_root_type")).isEqualTo("Todo"),
                        () -> assertThat(tAggregateRootResultSet.getLong(
                                "last_version")).isEqualTo
                                (1),
                        () -> assertThat(tAggregateRootResultSet.getString("aggregate_root_payload")).startsWith("\\x"),
                        () -> assertThat(tAggregateRootResultSet.getString(
                                "owned_by")).isEqualTo("Damien"),
                        () -> assertThat(tAggregateRootResultSet.getString(
                                "belongs_to")).isEqualTo("Damien-000002"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(3)
    void shouldLoadOrderByVersionASC() {
        // Given
        List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE)));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        new TodoId("Damien", TodoId.SEQUENCE_NUMBER_3),
                        "lorem ipsum",
                        Status.DONE,
                        false
                ), ExecutedBy.NotAvailable.INSTANCE);

        // When
        final List<ExecutedByEvent<TodoId>> events = todoEventRepository.loadOrderByVersionASC(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_3));

        // Then
        assertThat(events).containsExactly(
                new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE),
                new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE));
    }

    @Test
    @Order(4)
    void shouldPartialLoadAggregate() {
        // Given
        List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE)));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        new TodoId("Damien", TodoId.SEQUENCE_NUMBER_4),
                        "lorem ipsum",
                        Status.DONE,
                        false
                ), ExecutedBy.NotAvailable.INSTANCE);

        // When
        final List<ExecutedByEvent<TodoId>> events = todoEventRepository.loadOrderByVersionASC(
                new TodoId("Damien", TodoId.SEQUENCE_NUMBER_4), new AggregateVersion(1));

        // Then
        assertThat(events).containsExactly(
                new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE),
                new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE));
    }

    @Test
    void shouldFailWhenEventIsAlreadyPresent() throws SQLException {
        // Given
        insertEvent("00000000-0000-0000-0000-000000000006", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        // When && Then
        assertThatThrownBy(() -> insertEvent("00000000-0000-0000-0000-000000000006", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB))
                .isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: Event already present while should not be ! aggregate_root_id 00000000-0000-0000-0000-000000000006 aggregate_root_type Todo");
    }

    @Test
    void shouldFailWhenNewVersionIsNotPreviousOnePlusOne() throws SQLException {
        // Given
        insertEvent("00000000-0000-0000-0000-000000000007", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        // When && Then
        assertThatThrownBy(() -> {
            insertEvent("00000000-0000-0000-0000-000000000007", "Todo", 2,
                    Instant.parse("2025-10-13T18:01:00Z"), "TodoMarkedAsDone", "\\x",
                    new OwnedBy("Damien"), BOB);
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: current version unexpected 2 - expected version 1");
    }

    @Test
    void shouldPreventMutabilityByFailingToUpdateAnEventAggregateType() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000008", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE event SET event_payload = '{\"description\": \"lorem ipsum\"}'
                                 WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000008'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToUpdateAnEventAggregateId() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000009", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE event SET aggregate_root_id = '00000000-0000-0000-0000-000000000019'
                                 WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000009'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToUpdateAnEventVersion() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000010", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE event SET version = 1 WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000010'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToUpdateAnEventStoredAt() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000011", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE event SET stored_at = ?
                                 WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000011'
                                 """)) {
                ps.setTimestamp(1, Timestamp.from(Instant.now()));
                try (final ResultSet rs = ps.executeQuery()) {
                }
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToUpdateAnEventType() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000012", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE event SET event_type = 'boom'
                                 WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000012'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToUpdateAnEventPayload() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000013", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE event SET event_payload = '{\"description\": \"lorem ipsum\"}'
                                 WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000013'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToUpdateAnEventBelongsTo() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000014", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE event SET belongs_to = 'Damien'
                                 WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000014'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToUpdateAnEventOwnedBy() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000015", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE event SET owned_by = 'Alban'
                                 WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000015'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToUpdateAnEventExecutedBy() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000016", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE event SET executed_by = 'EU:alice'
                                 WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000016'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToDeleteAnEvent() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000017", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 DELETE FROM event WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000017'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldStoreExecutedByAnonymous() throws SQLException {
        // Given

        // When
        insertEvent("00000000-0000-0000-0000-000000000018", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), ExecutedBy.Anonymous.INSTANCE);

        // Then
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement tEventPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT executed_by FROM event WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000018'
                             """)) {
            try (final ResultSet tEventResultSet = tEventPreparedStatement.executeQuery()) {
                tEventResultSet.next();
                assertThat(tEventResultSet.getString("executed_by")).isEqualTo("A");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldStoreExecutedByEndUser() throws SQLException {
        // Given

        // When
        insertEvent("00000000-0000-0000-0000-000000000019", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), BOB);

        // Then
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement tEventPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT executed_by FROM event WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000019'
                             """)) {
            try (final ResultSet tEventResultSet = tEventPreparedStatement.executeQuery()) {
                tEventResultSet.next();
                assertThat(tEventResultSet.getString("executed_by")).isEqualTo("EU:encodedbob");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldStoreExecutedByServiceAccount() throws SQLException {
        // Given

        // When
        insertEvent("00000000-0000-0000-0000-000000000020", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), new ExecutedBy.ServiceAccount("service-account-quarkus-app"));

        // Then
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement tEventPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT executed_by FROM event WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000020'
                             """)) {
            try (final ResultSet tEventResultSet = tEventPreparedStatement.executeQuery()) {
                tEventResultSet.next();
                assertThat(tEventResultSet.getString("executed_by")).isEqualTo("SA:service-account-quarkus-app");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldStoreExecutedByNotAvailable() throws SQLException {
        // Given

        // When
        insertEvent("00000000-0000-0000-0000-000000000021", "Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "NewTodoCreated", "\\x",
                new OwnedBy("Damien"), ExecutedBy.NotAvailable.INSTANCE);

        // Then
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement tEventPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT executed_by FROM event WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000021'
                             """)) {
            try (final ResultSet tEventResultSet = tEventPreparedStatement.executeQuery()) {
                tEventResultSet.next();
                assertThat(tEventResultSet.getString("executed_by")).isEqualTo("NA");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldPreventMutabilityByFailingToTruncateEventStoreTable() {
        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 TRUNCATE TABLE event
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldFindLastVersionById() {
        // Given
        List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE)));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        new TodoId("Damien", TodoId.SEQUENCE_NUMBER_5),
                        "lorem ipsum",
                        Status.DONE,
                        false
                ), ExecutedBy.NotAvailable.INSTANCE);

        // When
        final Optional<AggregateVersion> version = todoEventRepository.findLastAggregateVersionById(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_5));

        // Then
        assertThat(version).hasValue(new AggregateVersion(1));
    }

    @Test
    void shouldFindEventMetadataByIdOrderByVersionASC() throws SQLException {
        // Given
        List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE)));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        new TodoId("Damien", TodoId.SEQUENCE_NUMBER_6),
                        "lorem ipsum",
                        Status.DONE,
                        false
                ), ExecutedBy.NotAvailable.INSTANCE);

        // When
        final List<EventMetadata> eventMetadataByIdOrderByVersionASC = todoEventRepository.findEventMetadataByIdOrderByVersionASC(
                new TodoId("Damien", TodoId.SEQUENCE_NUMBER_6));

        // Then
        assertThat(eventMetadataByIdOrderByVersionASC).containsExactly(
                new EventMetadata(
                        "Todo", "NewTodoCreated", new AggregateVersion(0),
                        Timestamp.valueOf(LocalDateTime.of(2025, Month.OCTOBER, 13, 20, 0, 0)),
                        new OwnedBy("Damien"), new BelongsTo(new AnyAggregateId("Damien-000006")), ExecutedBy.NotAvailable.INSTANCE),
                new EventMetadata(
                        "Todo", "TodoMarkedAsDone", new AggregateVersion(1),
                        Timestamp.valueOf(LocalDateTime.of(2025, Month.OCTOBER, 13, 20, 0, 0)),
                        new OwnedBy("Damien"), new BelongsTo(new AnyAggregateId("Damien-000006")), ExecutedBy.NotAvailable.INSTANCE));
    }

    @Test
    void shouldFindEventMetadataByIdAndEventsOrderByVersionASC() {
        // Given
        List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE)));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        new TodoId("Damien", TodoId.SEQUENCE_NUMBER_7),
                        "lorem ipsum",
                        Status.DONE,
                        false
                ), ExecutedBy.NotAvailable.INSTANCE);

        // When
        final List<EventMetadata> eventMetadataByIdOrderByVersionASC = todoEventRepository.findEventMetadataByIdAndEventsOrderByVersionASC(
                new TodoId("Damien", TodoId.SEQUENCE_NUMBER_7), List.of(TodoMarkedAsDone.class));

        // Then
        assertThat(eventMetadataByIdOrderByVersionASC).containsExactly(new EventMetadata(
                "Todo", "TodoMarkedAsDone", new AggregateVersion(1),
                Timestamp.valueOf(LocalDateTime.of(2025, Month.OCTOBER, 13, 20, 0, 0)),
                new OwnedBy("Damien"), new BelongsTo(new AnyAggregateId("Damien-000007")), ExecutedBy.NotAvailable.INSTANCE));
    }

    @Test
    void shouldReturnFalseWhenNoEventIsPresent() {
        // Given

        // When
        final boolean hasEventsFor = todoEventRepository.hasEventsFor(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_8));

        // Then
        assertThat(hasEventsFor).isFalse();
    }

    @Test
    void shouldReturnTrueWhenAtLeastOneEventIsPresent() {
        // Given
        List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE)));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        new TodoId("Damien", TodoId.SEQUENCE_NUMBER_9),
                        "lorem ipsum",
                        Status.DONE,
                        false
                ), ExecutedBy.NotAvailable.INSTANCE);

        // When
        final boolean hasEventsFor = todoEventRepository.hasEventsFor(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_9));

        // Then
        assertThat(hasEventsFor).isTrue();
    }

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
            preparedStatement.setString(9, executedBy.encode(TestExecutedByEncoder.INSTANCE));
            preparedStatement.executeUpdate();
        }
    }
}
