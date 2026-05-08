package com.damdamdeo.pulse.extension.common.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "pulse")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CommonConfiguration {

    /**
     * Datasource configuration
     */
    DatasourceConfiguration datasource();

    @ConfigGroup
    interface DatasourceConfiguration {

        /**
         * init at startup
         */
        @WithDefault("true")
        boolean initAtStartup();
    }
}
