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

@ApplicationScoped
@Unremovable
public class DatabaseInitializationService {

    private static final Logger LOG = Logger.getLogger(DatabaseInitializationService.class);

    @Inject
    InitializationScriptsProvider initializationScriptsProvider;

    @Inject
    Provider<DataSource> dataSource;

    void onStart(@Observes StartupEvent ev) {
        executeScripts();
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
