package com.damdamdeo.pulse.extension.test;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.runtime.InstantProvider;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcPostgresEventRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .withConfigurationResource("application.properties");

    @Inject
    EventRepository<Todo, TodoId> todoEventRepository;

    @Inject
    DataSource dataSource;

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
    @Order(0)
    void shouldTablesBeInitialized() {
        // Given

        // When
        final List<String> tables = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT table_schema, table_name
                                 FROM information_schema.tables
                                 ORDER BY table_schema, table_name
                             """);
             final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String schema = rs.getString("table_schema");
                String table = rs.getString("table_name");
                tables.add(schema + "." + table);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Then
        assertThat(tables).contains("todotaking_todo.t_event", "todotaking_todo.t_aggregate_root");
    }

    @Test
    @Order(1)
    void shouldSave() {
        // Given
        final List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new NewTodoCreated(new TodoId("Damien", 1L), "lorem ipsum")));

        // When
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        new TodoId("Damien", 1L),
                        "lorem ipsum",
                        Status.IN_PROGRESS,
                        false
                ));

        // Then
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement tEventPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT aggregate_root_id, aggregate_root_type, version, creation_date, event_type, event_payload, owned_by
                                 FROM t_event WHERE aggregate_root_id = ? AND aggregate_root_type = ?
                             """);
             final PreparedStatement tAggregateRootPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT aggregate_root_id, aggregate_root_type, last_version, aggregate_root_payload, owned_by, in_relation_with
                                 FROM t_aggregate_root WHERE aggregate_root_id = ? AND aggregate_root_type = ?
                             """)) {
            tEventPreparedStatement.setString(1, "Damien/1");
            tEventPreparedStatement.setString(2, "com.damdamdeo.pulse.extension.core.Todo");
            tAggregateRootPreparedStatement.setString(1, "Damien/1");
            tAggregateRootPreparedStatement.setString(2, "com.damdamdeo.pulse.extension.core.Todo");
            try (final ResultSet tEventResultSet = tEventPreparedStatement.executeQuery();
                 final ResultSet tAggregateRootResultSet = tAggregateRootPreparedStatement.executeQuery()) {
                tEventResultSet.next();
                tAggregateRootResultSet.next();
                assertAll(
                        () -> assertThat(tEventResultSet.getString("aggregate_root_id")).isEqualTo("Damien/1"),
                        () -> assertThat(tEventResultSet.getString("aggregate_root_type")).isEqualTo("com.damdamdeo.pulse.extension.core.Todo"),
                        () -> assertThat(tEventResultSet.getLong("version")).isEqualTo(0),
                        () -> assertThat(tEventResultSet.getString("creation_date")).isEqualTo("2025-10-13 20:00:00"),
                        () -> assertThat(tEventResultSet.getString("event_type")).isEqualTo("com.damdamdeo.pulse.extension.core.event.NewTodoCreated"),
                        () -> assertThat(tEventResultSet.getString("event_payload")).startsWith("\\x"),
                        () -> assertThat(tEventResultSet.getString("owned_by")).isEqualTo("Damien"),

                        () -> assertThat(tAggregateRootResultSet.getString("aggregate_root_id")).isEqualTo(
                                "Damien/1"),
                        () -> assertThat(tAggregateRootResultSet.getString(
                                "aggregate_root_type")).isEqualTo("com.damdamdeo.pulse.extension.core.Todo"),
                        () -> assertThat(tAggregateRootResultSet.getLong(
                                "last_version")).isEqualTo
                                (0),
                        () -> assertThat(tAggregateRootResultSet.getString("aggregate_root_payload")).startsWith("\\x"),
                        () -> assertThat(tAggregateRootResultSet.getString("owned_by")).isEqualTo("Damien"),
                        () -> assertThat(tAggregateRootResultSet.getString("in_relation_with")).isEqualTo("Damien/1"));
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
                                new NewTodoCreated(new TodoId("Damien", 2L), "lorem ipsum")
                        )),
                new Todo(
                        new TodoId("Damien", 2L),
                        "lorem ipsum",
                        Status.IN_PROGRESS,
                        false
                ));

        // When
        todoEventRepository.save(List.of(
                        new VersionizedEvent<>(new AggregateVersion(1),
                                new TodoMarkedAsDone(new TodoId("Damien", 2L))
                        )),
                new Todo(
                        new TodoId("Damien", 2L),
                        "lorem ipsum",
                        Status.DONE,
                        false
                ));

        // Then
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement tEventPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT aggregate_root_id, aggregate_root_type, version, creation_date, event_type, event_payload, owned_by
                                 FROM t_event WHERE aggregate_root_id = ? AND aggregate_root_type = ?
                             """);
             final PreparedStatement tAggregateRootPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT aggregate_root_id, aggregate_root_type, last_version, aggregate_root_payload, owned_by, in_relation_with
                                 FROM t_aggregate_root WHERE aggregate_root_id = ? AND aggregate_root_type = ?
                             """)) {
            tEventPreparedStatement.setString(1, "Damien/2");
            tEventPreparedStatement.setString(2, "com.damdamdeo.pulse.extension.core.Todo");
            tAggregateRootPreparedStatement.setString(1, "Damien/2");
            tAggregateRootPreparedStatement.setString(2, "com.damdamdeo.pulse.extension.core.Todo");
            try (final ResultSet tEventResultSet = tEventPreparedStatement.executeQuery();
                 final ResultSet tAggregateRootResultSet = tAggregateRootPreparedStatement.executeQuery()) {
                tEventResultSet.next();
                assertAll(
                        () -> assertThat(tEventResultSet.getString("aggregate_root_id")).isEqualTo("Damien/2"),
                        () -> assertThat(tEventResultSet.getString("aggregate_root_type")).isEqualTo("com.damdamdeo.pulse.extension.core.Todo"),
                        () -> assertThat(tEventResultSet.getLong("version")).isEqualTo(0),
                        () -> assertThat(tEventResultSet.getString("creation_date")).isEqualTo("2025-10-13 20:00:00"),
                        () -> assertThat(tEventResultSet.getString("event_type")).isEqualTo("com.damdamdeo.pulse.extension.core.event.NewTodoCreated"),
                        () -> assertThat(tEventResultSet.getString("event_payload")).startsWith("\\x"),
                        () -> assertThat(tEventResultSet.getString("owned_by")).isEqualTo("Damien"));
                tEventResultSet.next();
                assertAll(
                        () -> assertThat(tEventResultSet.getString("aggregate_root_id")).isEqualTo("Damien/2"),
                        () -> assertThat(tEventResultSet.getString("aggregate_root_type")).isEqualTo("com.damdamdeo.pulse.extension.core.Todo"),
                        () -> assertThat(tEventResultSet.getLong("version")).isEqualTo(1),
                        () -> assertThat(tEventResultSet.getString("creation_date")).isEqualTo("2025-10-13 20:00:00"),
                        () -> assertThat(tEventResultSet.getString("event_type")).isEqualTo("com.damdamdeo.pulse.extension.core.event.TodoMarkedAsDone"),
                        () -> assertThat(tEventResultSet.getString("event_payload")).startsWith("\\x"),
                        () -> assertThat(tEventResultSet.getString("owned_by")).isEqualTo("Damien"));
                tAggregateRootResultSet.next();
                assertAll(
                        () -> assertThat(tAggregateRootResultSet.getString("aggregate_root_id")).isEqualTo(
                                "Damien/2"),
                        () -> assertThat(tAggregateRootResultSet.getString(
                                "aggregate_root_type")).isEqualTo("com.damdamdeo.pulse.extension.core.Todo"),
                        () -> assertThat(tAggregateRootResultSet.getLong(
                                "last_version")).isEqualTo
                                (1),
                        () -> assertThat(tAggregateRootResultSet.getString("aggregate_root_payload")).startsWith("\\x"),
                        () -> assertThat(tAggregateRootResultSet.getString(
                                "owned_by")).isEqualTo("Damien"),
                        () -> assertThat(tAggregateRootResultSet.getString(
                                "in_relation_with")).isEqualTo("Damien/2"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Must have the property *quarkus.class-loading.parent-first-artifacts* defined to *com.damdamdeo:pulse-core:tests*.
     * Events created from store will not belong to the same classloader as events created in this test.
     */
    @Test
    @Order(3)
    void shouldLoadOrderByVersionASC() {
        // Given
        List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new NewTodoCreated(new TodoId("Damien", 3L), "lorem ipsum")),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new TodoMarkedAsDone(new TodoId("Damien", 3L))));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        new TodoId("Damien", 3L),
                        "lorem ipsum",
                        Status.DONE,
                        false
                ));

        // When
        final List<Event<TodoId>> events = todoEventRepository.loadOrderByVersionASC(new TodoId("Damien", 3L));

        // Then
        assertThat(events).containsExactly(
                new NewTodoCreated(new TodoId("Damien", 3L), "lorem ipsum"),
                new TodoMarkedAsDone(new TodoId("Damien", 3L)));
    }

    @Test
    @Order(4)
    void shouldPartialLoadAggregate() {
        // Given
        List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new NewTodoCreated(new TodoId("Damien", 4L), "lorem ipsum")),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new TodoMarkedAsDone(new TodoId("Damien", 4L))));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        new TodoId("Damien", 4L),
                        "lorem ipsum",
                        Status.DONE,
                        false
                ));

        // When
        final List<Event<TodoId>> events = todoEventRepository.loadOrderByVersionASC(
                new TodoId("Damien", 4L), new AggregateVersion(1));

        // Then
        assertThat(events).containsExactly(
                new NewTodoCreated(new TodoId("Damien", 4L), "lorem ipsum"),
                new TodoMarkedAsDone(new TodoId("Damien", 4L)));
    }

    @Test
    void shouldFailWhenEventIsAlreadyPresent() throws SQLException {
        // Given
        insertEvent("00000000-0000-0000-0000-000000000006", "com.damdamdeo.pulse.extension.core.Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "com.damdamdeo.pulse.extension.core.event.NewTodoCreated", "\\x",
                new OwnedBy("Damien"));

        // When && Then
        assertThatThrownBy(() -> insertEvent("00000000-0000-0000-0000-000000000006", "com.damdamdeo.pulse.extension.core.Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "com.damdamdeo.pulse.extension.core.event.NewTodoCreated", "\\x",
                new OwnedBy("Damien")))
                .isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: Event already present while should not be ! aggregate_root_id 00000000-0000-0000-0000-000000000006 aggregate_root_type com.damdamdeo.pulse.extension.core.Todo");
    }

    @Test
    void shouldFailWhenNewVersionIsNotPreviousOnePlusOne() throws SQLException {
        // Given
        insertEvent("00000000-0000-0000-0000-000000000007", "com.damdamdeo.pulse.extension.core.Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "com.damdamdeo.pulse.extension.core.event.NewTodoCreated", "\\x",
                new OwnedBy("Damien"));

        // When && Then
        assertThatThrownBy(() -> {
            insertEvent("00000000-0000-0000-0000-000000000007", "com.damdamdeo.pulse.extension.core.Todo", 2,
                    Instant.parse("2025-10-13T18:01:00Z"), "com.damdamdeo.pulse.extension.core.TodoMarkedAsDone", "\\x",
                    new OwnedBy("Damien"));
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: current version unexpected 2 - expected version 1");
    }

    @Test
    void shouldPreventMutabilityByFailingToUpdateAnEvent() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000008", "com.damdamdeo.pulse.extension.core.Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "com.damdamdeo.pulse.extension.core.event.NewTodoCreated", "\\x",
                new OwnedBy("Damien"));

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE t_event SET event_payload = '{\"id\": \"00000000-0000-0000-0000-000000000008\", \"description\": \"lorem ipsum\"}'
                                 WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000008'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToDeleteAnEvent() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000009", "com.damdamdeo.pulse.extension.core.Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "com.damdamdeo.pulse.extension.core.event.NewTodoCreated", "\\x",
                new OwnedBy("Damien"));

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 DELETE FROM t_event WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000009'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToTruncateEventStoreTable() {
        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 TRUNCATE TABLE t_event
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    private void insertEvent(final String aggregateRootId, final String aggregateRootType, final Integer version,
                             final Instant creationDate, final String eventType, final String encryptedEventPayload,
                             final OwnedBy ownedBy) throws SQLException {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             INSERT INTO t_event (aggregate_root_id, aggregate_root_type, version, creation_date, event_type, event_payload, owned_by) 
                             VALUES (?, ?, ?, ?, ?, ?, ?)
                             """)) {
            preparedStatement.setString(1, aggregateRootId);
            preparedStatement.setString(2, aggregateRootType);
            preparedStatement.setLong(3, version);
            preparedStatement.setTimestamp(4, Timestamp.from(creationDate));
            preparedStatement.setString(5, eventType);
            preparedStatement.setBytes(6, encryptedEventPayload.getBytes(StandardCharsets.UTF_8));
            preparedStatement.setString(7, ownedBy.id());
            preparedStatement.executeUpdate();
        }
    }
}
