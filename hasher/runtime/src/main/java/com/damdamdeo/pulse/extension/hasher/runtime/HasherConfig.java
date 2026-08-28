package com.damdamdeo.pulse.extension.hasher.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "pulse.hasher")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HasherConfig {

    /**
     * pepper to apply on value to be hashed
     *
     * @return pepper
     */
    Optional<String> pepper();
}
