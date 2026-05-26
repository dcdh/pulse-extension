package com.damdamdeo.pulse.extension.common.runtime.datasource;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class DatabaseInitializationService {

    private static final Logger LOG = Logger.getLogger(DatabaseInitializationService.class);

    @Inject
    InitializationScriptsProvider initializationScriptsProvider;

    @Inject
    Provider<DataSource> dataSource;

    private final DataSourceConfiguration dataSourceConfiguration;

    public DatabaseInitializationService(final DataSourceConfiguration dataSourceConfiguration) {
        this.dataSourceConfiguration = Objects.requireNonNull(dataSourceConfiguration);
    }

    void onStart(@Observes final StartupEvent ev) {
        if (dataSourceConfiguration.initAtStartup()) {
            executeScripts();
        }
    }

    private void executeScripts() {
        List<PostgresSqlScript> postgresSqlScripts = initializationScriptsProvider.provide();
        try (final Connection connection = dataSource.get().getConnection()) {
            connection.setAutoCommit(false);
            for (final PostgresSqlScript postgresSqlScript : postgresSqlScripts) {
                executeScript(connection, postgresSqlScript);
            }
            connection.commit();
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to execute SQL scripts", e);
        }
    }

    private void executeScript(final Connection connection, final PostgresSqlScript script) throws SQLException {
        LOG.infov("Executing initialization script ''{0}''", script.name());
        try (final Statement st = connection.createStatement()) {
            st.execute(script.content());
        }
    }
}
