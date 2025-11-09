package com.damdamdeo.pulse.extension.common.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

import java.util.List;

public class CachingProcessor {

    @BuildStep
    List<RunTimeConfigurationDefaultBuildItem> defineDefaultConfiguration() {
        return List.of(
                new RunTimeConfigurationDefaultBuildItem("quarkus.cache.caffeine.\"passphrase\".initial-capacity", "10000"),
                new RunTimeConfigurationDefaultBuildItem("quarkus.cache.caffeine.\"passphrase\".maximum-size", "10000"),
                new RunTimeConfigurationDefaultBuildItem("quarkus.cache.caffeine.\"passphrase\".expire-after-write", "7D"));
    }
}
