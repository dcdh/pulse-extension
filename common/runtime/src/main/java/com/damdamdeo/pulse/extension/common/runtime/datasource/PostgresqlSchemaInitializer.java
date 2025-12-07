package com.damdamdeo.pulse.extension.common.runtime.datasource;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

@Singleton
@Unremovable
public class PostgresqlSchemaInitializer {

    private final DataSource dataSource;
    private final FromApplication fromApplication;

    public PostgresqlSchemaInitializer(final DataSource dataSource,
                                       @ConfigProperty(name = "quarkus.application.name") final String quarkusApplicationName) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.fromApplication = FromApplication.from(quarkusApplicationName);
    }

    public void onStart(@Observes @Priority(10) final StartupEvent event) throws Exception {
        try (final Connection con = dataSource.getConnection();
             final Statement stmt = con.createStatement()) {
            con.setAutoCommit(false);
            stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS %s;".formatted(fromApplication.value().toLowerCase()));
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}

