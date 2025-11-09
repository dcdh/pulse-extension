package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "pulse.debezium")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DebeziumConfiguration {

    /**
     * Enable the debezium configuration
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Connect configuration
     */
    ConnectConfiguration connect();

    @ConfigGroup
    interface ConnectConfiguration {

        /**
         * Postgres Configuration
         */
        PostgresConfiguration postgres();

        /**
         * Debezium connect's port
         */
        Optional<Integer> port();

        @ConfigGroup
        interface PostgresConfiguration {

            /**
             * dbName - default to quarkus
             */
            @WithDefault("quarkus")
            String dbName();

            /**
             * Network name - default to postgres in dev
             */
            @WithDefault("postgres")
            String networkName();

            /**
             * port - default to 5432
             */
            @WithDefault("5432")
            Integer port();
        }
    }
}
