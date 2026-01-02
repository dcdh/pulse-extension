package com.damdamdeo.pulse.extension.consumer.deployment.idempotency;

import com.damdamdeo.pulse.extension.consumer.runtime.idempotency.JdbcPostgresIdempotencyRepository;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyKey;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.Topic;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JdbcPostgresIdempotencyRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresIdempotencyRepository jdbcPostgresIdempotencyRepository;

    @Inject
    DataSource dataSource;

    @AfterEach
    void tearDown() {
        try (final Connection connection = dataSource.getConnection();
             final Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE t_idempotency");
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

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
        assertThat(tables).contains("todotaking_todo.t_idempotency");
    }

    @Test
    void shouldFindLastAggregateVersionByReturnEmptyWhenNotPresent() {
        // Given

        // When
        final Optional<LastConsumedAggregateVersion> lastAggregateVersionBy = jdbcPostgresIdempotencyRepository.findLastAggregateVersionBy(
                new IdempotencyKey(
                        new Purpose("statistics"),
                        new FromApplication("TodoTaking", "Todo"),
                        Topic.EVENT,
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Damien/0")));

        // Then
        assertThat(lastAggregateVersionBy).isEmpty();
    }

    @Test
    void shouldFindLastAggregateVersionByReturnLastConsumedAggregateVersionWhenPresent() {
        // Given
        // language=sql
        final String sql = """
                    INSERT INTO t_idempotency (purpose, from_application, topic, aggregate_root_type, aggregate_root_id, last_consumed_version)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "statistics");
            ps.setString(2, new FromApplication("TodoTaking", "Todo").value());
            ps.setString(3, Topic.EVENT.name());
            ps.setString(4, Todo.class.getSimpleName());
            ps.setString(5, "Damien/0");
            ps.setInt(6, 0);
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        // When
        final Optional<LastConsumedAggregateVersion> lastAggregateVersionBy = jdbcPostgresIdempotencyRepository.findLastAggregateVersionBy(
                new IdempotencyKey(
                        new Purpose("statistics"),
                        new FromApplication("TodoTaking", "Todo"),
                        Topic.EVENT,
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Damien/0")));

        // Then
        assertThat(lastAggregateVersionBy).isEqualTo(Optional.of(
                new LastConsumedAggregateVersion(0)));
    }

    @Test
    void shouldUpsertInsertWhenNotPresent() {
        // Given

        // When
        jdbcPostgresIdempotencyRepository.upsert(
                new IdempotencyKey(
                        new Purpose("statistics"),
                        new FromApplication("TodoTaking", "Todo"),
                        Topic.EVENT,
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Damien/0")), new CurrentVersionInConsumption(0));

        // Then
        // language=sql
        final String sql = """
                    SELECT last_consumed_version FROM t_idempotency
                    WHERE purpose = ? AND from_application = ? AND topic = ? AND aggregate_root_type = ? AND aggregate_root_id = ?
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "statistics");
            ps.setString(2, new FromApplication("TodoTaking", "Todo").value());
            ps.setString(3, Topic.EVENT.name());
            ps.setString(4, Todo.class.getSimpleName());
            ps.setString(5, "Damien/0");
            try (final ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt("last_consumed_version")).isEqualTo(0);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldUpsertUpdateWhenPresent() {
        // Given
        // language=sql
        final String sql = """
                    INSERT INTO t_idempotency (purpose, from_application, topic, aggregate_root_type, aggregate_root_id, last_consumed_version)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "statistics");
            ps.setString(2, new FromApplication("TodoTaking", "Todo").value());
            ps.setString(3, Topic.EVENT.name());
            ps.setString(4, Todo.class.getSimpleName());
            ps.setString(5, "Damien/0");
            ps.setInt(6, 0);
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        // When
        jdbcPostgresIdempotencyRepository.upsert(
                new IdempotencyKey(
                        new Purpose("statistics"),
                        new FromApplication("TodoTaking", "Todo"),
                        Topic.EVENT,
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Damien/0")), new CurrentVersionInConsumption(1));

        // Then
        // language=sql
        final String querySql = """
                    SELECT last_consumed_version FROM t_idempotency
                    WHERE purpose = ? AND from_application = ? AND topic = ? AND aggregate_root_type = ? AND aggregate_root_id = ?
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(querySql)) {
            ps.setString(1, "statistics");
            ps.setString(2, new FromApplication("TodoTaking", "Todo").value());
            ps.setString(3, Topic.EVENT.name());
            ps.setString(4, Todo.class.getSimpleName());
            ps.setString(5, "Damien/0");
            try (final ResultSet rs = ps.executeQuery()) {
                assertAll(
                        () -> assertThat(rs.next()).isTrue(),
                        () -> assertThat(rs.getInt("last_consumed_version")).isEqualTo(1)
                );
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
