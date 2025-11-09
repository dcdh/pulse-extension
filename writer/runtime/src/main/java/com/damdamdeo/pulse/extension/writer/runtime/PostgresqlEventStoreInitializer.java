package com.damdamdeo.pulse.extension.writer.runtime;

import io.quarkus.arc.Unremovable;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.faulttolerance.api.TypedGuard;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Unremovable
public final class PostgresqlEventStoreInitializer {

    private static final TypedGuard<Void> GUARD = TypedGuard.create(Void.class)
            .withRetry().delay(1, TimeUnit.SECONDS.toChronoUnit())
            .maxRetries(3)
            .done()
            .build();

    private static final String POSTGRESQL_DDL_FILE = "/sql/event-store-postgresql.ddl";

    private final DataSource dataSource;

    public PostgresqlEventStoreInitializer(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    public void onStart(@Observes @Priority(20) final StartupEvent event) throws Exception {
        // Use a retry mechanism in case of multiple instances running in //
        // DDL will be started each time an instance of the application is started
        // DDL must use an IF NOT EXIST syntax
        final InputStream ddlResource = Objects.requireNonNull(this.getClass().getResourceAsStream(POSTGRESQL_DDL_FILE));
        final String ddl = new String(ddlResource.readAllBytes(), StandardCharsets.UTF_8);
        GUARD.call(() ->
                QuarkusTransaction.requiringNew().call(() -> {
                    try (final Connection con = dataSource.getConnection();
                         final Statement stmt = con.createStatement()) {
                        con.setAutoCommit(false);
                        stmt.executeUpdate(ddl);
                        stmt.executeUpdate("CREATE EXTENSION IF NOT EXISTS pgcrypto;");
                    } catch (final SQLException exception) {
                        throw new RuntimeException(exception);
                    }
                    return null;
                }));
    }
}
