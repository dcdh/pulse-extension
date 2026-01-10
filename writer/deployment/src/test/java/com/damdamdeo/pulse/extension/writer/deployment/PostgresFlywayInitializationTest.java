package com.damdamdeo.pulse.extension.writer.deployment;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresFlywayInitializationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-flyway", Version.getVersion()),
                    Dependency.of("org.flywaydb", "flyway-database-postgresql", "11.12.0")
            ));

    @Inject
    DataSource dataSource;

    @Test
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
        assertThat(tables).contains("todotaking_todo.event", "todotaking_todo.aggregate_root",
                "todotaking_todo.flyway_schema_history");
    }

    @Test
    void shouldHaveInitializeFlywaySchemaHistoryWithPulseInitializationScript() {
        // Given
        boolean found = false;

        // When
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT version, description, success
                             FROM todotaking_todo.flyway_schema_history
                             ORDER BY installed_rank
                             """
             );
             final ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                final String version = rs.getString("version");
                final String description = rs.getString("description");
                final boolean success = rs.getBoolean("success");

                if ("0".equals(version)
                        && "pulse initialisation".equalsIgnoreCase(description)
                        && success) {
                    found = true;
                    break;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Then
        assertThat(found)
                .as("Flyway schema history should contain Pulse initialization migration V0")
                .isTrue();
    }
}
