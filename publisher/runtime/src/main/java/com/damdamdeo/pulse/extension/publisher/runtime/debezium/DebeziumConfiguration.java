package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

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

    /**
     * Connect Topic creation
     */
    TopicCreation topicCreation();

    @ConfigGroup
    interface ConnectConfiguration {

        /**
         * Debezium connect's host - default to localhost
         */
        @WithDefault("localhost")
        String host();

        /**
         * Debezium connect's port - default to 8083
         */
        @WithDefault("8083")
        Integer port();
    }

    @ConfigGroup
    interface TopicCreation {

        /**
         * Topic creation default partitions
         */
        @WithDefault("1")
        Integer defaultPartitions();
    }
}
