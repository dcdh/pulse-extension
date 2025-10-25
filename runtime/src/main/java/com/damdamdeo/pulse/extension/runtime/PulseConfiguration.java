package com.damdamdeo.pulse.extension.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Map;

@ConfigMapping(prefix = "pulse")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface PulseConfiguration {

    /**
     * Target Topic binding
     *
     * @return
     */
    @WithName("target-topic-binding")
    Map<String, String> targetTopicBinding();
}
