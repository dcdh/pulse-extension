package com.damdamdeo.pulse.extension.deployment;

import com.damdamdeo.pulse.extension.runtime.CaffeineCacheRepository;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

public class CacheProcessor {

    @BuildStep
    RunTimeConfigurationDefaultBuildItem cacheInitialCapacity() {
        return new RunTimeConfigurationDefaultBuildItem(
                "quarkus.cache.caffeine.\"query-event-store\".initial-capacity",
                "10000"
        );
    }

    @BuildStep
    RunTimeConfigurationDefaultBuildItem cacheMaximumSize() {
        return new RunTimeConfigurationDefaultBuildItem(
                "quarkus.cache.caffeine.\"query-event-store\".maximum-size",
                "10000"
        );
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeanBuildItem() {
        return AdditionalBeanBuildItem.builder().addBeanClass(CaffeineCacheRepository.class)
                .build();
    }
}
