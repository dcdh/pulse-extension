package com.damdamdeo.pulse.extension.runtime.consumer;

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
}
