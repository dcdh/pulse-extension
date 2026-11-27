package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.traceability.TracingMode;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * pulse traceability configuration
 */
@ConfigMapping(prefix = "pulse.traceability")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface TraceabilityConfiguration {

    /**
     * Tracing mode
     * @return
     */
    @WithDefault("DISABLED")
    TracingMode tracingMode();

    /**
     * role
     *
     * @return
     */
    @WithDefault("ROLE_TRACEABILITY_READ")
    String role();
}
