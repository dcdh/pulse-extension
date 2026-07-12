package com.damdamdeo.pulse.extension.query.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

import java.util.List;

@ConfigMapping(prefix = "pulse.backend-user.visibility")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface BackendUserVisibilityRolesConfig {

    /**
     * Roles that are allowed to execute the Query.
     */
    List<String> roles();
}
