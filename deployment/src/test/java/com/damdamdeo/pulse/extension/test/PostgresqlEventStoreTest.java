package com.damdamdeo.pulse.extension.test;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.postgresql.util.PSQLException;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresqlEventStoreTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Test
    void shouldTableBeInitialized() {
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
        assertThat(tables).contains("public.t_event");
    }

    @Test
    void shouldFailWhenEventIsAlreadyPresent() throws SQLException {
        // Given
        insertEvent("00000000-0000-0000-0000-000000000002", "com.damdamdeo.pulse.extension.core.Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "com.damdamdeo.pulse.extension.core.event.NewTodoCreated",
                "{\"id\": {\"id\": \"00000000-0000-0000-0000-000000000002\"}, \"description\": \"lorem ipsum\"}");

        // When && Then
        assertThatThrownBy(() -> insertEvent("00000000-0000-0000-0000-000000000002", "com.damdamdeo.pulse.extension.core.Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "com.damdamdeo.pulse.extension.core.event.NewTodoCreated",
                "{\"id\": {\"id\": \"00000000-0000-0000-0000-000000000002\"}, \"description\": \"lorem ipsum\"}"))
                .isInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: Event already present while should not be ! aggregate_root_id 00000000-0000-0000-0000-000000000002 aggregate_root_type com.damdamdeo.pulse.extension.core.Todo");
    }

    @Test
    void shouldFailWhenNewVersionIsNotPreviousOnePlusOne() throws SQLException {
        // Given
        insertEvent("00000000-0000-0000-0000-000000000003", "com.damdamdeo.pulse.extension.core.Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "com.damdamdeo.pulse.extension.core.event.NewTodoCreated",
                "{\"id\": {\"id\": \"00000000-0000-0000-0000-000000000003\"}, \"description\": \"lorem ipsum\"}");

        // When && Then
        assertThatThrownBy(() -> {
            insertEvent("00000000-0000-0000-0000-000000000003", "com.damdamdeo.pulse.extension.core.Todo", 2,
                    Instant.parse("2025-10-13T18:01:00Z"), "com.damdamdeo.pulse.extension.core.TodoMarkedAsDone",
                    "{\"id\": {\"id\": \"00000000-0000-0000-0000-000000000003\"}}");
        }).isInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: current version unexpected 2 - expected version 1");
    }

    @Test
    void shouldPreventMutabilityByFailingToUpdateAnEvent() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000004", "com.damdamdeo.pulse.extension.core.Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "com.damdamdeo.pulse.extension.core.event.NewTodoCreated",
                "{\"id\": {\"id\": \"00000000-0000-0000-0000-000000000004\"}, \"description\": \"lorem ipsum\"}");

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE public.t_event SET event_payload = '{\"id\": {\"id\": \"00000000-0000-0000-0000-000000000004\"}, \"description\": \"lorem ipsum 2\"}'
                                 WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000004'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToDeleteAnEvent() throws SQLException {
        insertEvent("00000000-0000-0000-0000-000000000005", "com.damdamdeo.pulse.extension.core.Todo", 0,
                Instant.parse("2025-10-13T18:00:00Z"), "com.damdamdeo.pulse.extension.core.event.NewTodoCreated",
                "{\"id\": {\"id\": \"00000000-0000-0000-0000-000000000005\"}, \"description\": \"lorem ipsum\"}");

        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 DELETE FROM public.t_event WHERE aggregate_root_id = '00000000-0000-0000-0000-000000000005'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    @Test
    void shouldPreventMutabilityByFailingToTruncateEventStoreTable() {
        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 TRUNCATE TABLE public.t_event
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: not allowed");
    }

    private void insertEvent(final String aggregateRootId, final String aggregateRootType, final Integer version,
                             final Instant creationDate, final String eventType, final String eventPayload) throws SQLException {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             INSERT INTO T_EVENT (aggregate_root_id, aggregate_root_type, version, creation_date, event_type, event_payload) 
                             VALUES (?, ?, ?, ?, ?, to_json(?::json))
                             """)) {
            preparedStatement.setString(1, aggregateRootId);
            preparedStatement.setString(2, aggregateRootType);
            preparedStatement.setLong(3, version);
            preparedStatement.setTimestamp(4, Timestamp.from(creationDate));
            preparedStatement.setString(5, eventType);
            preparedStatement.setString(6, eventPayload);

            preparedStatement.executeUpdate();
        }
    }
}
