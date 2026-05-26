package com.damdamdeo.pulse.extension.common.runtime.datasource;


import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Datasource configuration
 */
@ConfigMapping(prefix = "pulse.datasource")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DataSourceConfiguration {

    /**
     * init at startup
     */
    @WithDefault("true")
    boolean initAtStartup();
}
