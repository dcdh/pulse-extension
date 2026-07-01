package com.damdamdeo.pulse.extension.obfuscator.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CachingProcessor {

    private static final Map<String, String> CACHING_CONFIGURATIONS = Map.of(
            "quarkus.cache.caffeine.\"obfuscator\".initial-capacity", "10000",
            "quarkus.cache.caffeine.\"obfuscator\".maximum-size", "10000",
            "quarkus.cache.caffeine.\"obfuscator\".expire-after-write", "7D");

    @BuildStep
    List<RunTimeConfigurationDefaultBuildItem> defineDefaultConfiguration(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return CACHING_CONFIGURATIONS.entrySet().stream()
                    .map(e -> new RunTimeConfigurationDefaultBuildItem(e.getKey(), e.getValue()))
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<ContentBuildItem> contentBuildItems(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            final List<ContentBuildItem> contentBuildItems = new ArrayList<>();
            contentBuildItems.add(new ContentBuildItem(new Title(Title.Level.SECOND, "Obfuscator caching configuration")));
            contentBuildItems.add(new ContentBuildItem(CodeBlock.fromProperties(CACHING_CONFIGURATIONS)));
            return contentBuildItems;
        } else {
            return List.of();
        }
    }
}
