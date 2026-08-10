package com.damdamdeo.pulse.extension.query.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "pulse.query")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface PulseQueryConfig {

    /**
     * masterKey to encode / decode encrypted property
     */
    String masterKey();

    /**
     * backendUser
     *
     * @return backendUser
     */
    BackendUser backendUser();

    /**
     * file
     *
     * @return file
     */
    File file();

    interface BackendUser {

        /**
         * visibility
         *
         * @return visibility
         */
        Visibility visibility();
    }

    interface Visibility {

        /**
         * Roles that are allowed to execute the Query.
         */
        List<String> roles();
    }

    interface File {

        /**
         * filigrane
         *
         * @return filigrane
         */
        Optional<String> filigrane();
    }
}
