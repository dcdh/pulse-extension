package com.damdamdeo.pulse.extension.consumer.deployment;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

// quarkus.datasource.devservices.init-script-path=init.sql is not working anymore when using Compose
@Singleton
public class PostgresAggregateRootTableCreator {

    private final DataSource dataSource;

    public PostgresAggregateRootTableCreator(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    void onStart(@Observes @Priority(15) final StartupEvent event) {
        try (final Connection con = dataSource.getConnection();
             final Statement stmt = con.createStatement()) {
            con.setAutoCommit(false);
            stmt.executeUpdate(
                    // language=sql
                    """
                            CREATE TABLE t_aggregate_root (
                              aggregate_root_type character varying(255) not null,
                              aggregate_root_id character varying(255) not null,
                              last_version bigint not null,
                              aggregate_root_payload bytea NOT NULL CHECK (octet_length(aggregate_root_payload) <= 1000 * 1024),
                              owned_by character varying(255) not null,
                              in_relation_with character varying(255) not null,
                              CONSTRAINT t_aggregate_root_pkey PRIMARY KEY (aggregate_root_id, aggregate_root_type)
                            );
                            """);
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}
