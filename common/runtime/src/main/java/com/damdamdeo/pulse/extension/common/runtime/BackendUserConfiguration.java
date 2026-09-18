package com.damdamdeo.pulse.extension.common.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

import java.util.List;
import java.util.Optional;

/**
 * pulse common configuration
 */
@ConfigMapping(prefix = "pulse.backend-user")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface BackendUserConfiguration {

    /**
     * visibility
     *
     * @return visibility
     */
    Visibility visibility();

    interface Visibility {

        /**
         * Roles that are allowed to execute the Query.
         */
        Optional<List<String>> roles();
    }
}
