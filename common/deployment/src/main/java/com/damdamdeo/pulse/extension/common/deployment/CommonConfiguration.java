package com.damdamdeo.pulse.extension.common.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * pulse common configuration
 */
@ConfigMapping(prefix = "pulse")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface CommonConfiguration {

}
