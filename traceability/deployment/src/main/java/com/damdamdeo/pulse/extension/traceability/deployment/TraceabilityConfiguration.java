package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.traceability.TracingMode;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * pulse traceability configuration
 */
@ConfigMapping(prefix = "pulse.traceability")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface TraceabilityConfiguration {

    /**
     * Tracing mode
     * @return
     */
    @WithDefault("DISABLED")
    TracingMode tracingMode();
}
