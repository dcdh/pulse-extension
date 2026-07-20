package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.CachedPassphraseRepository;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CachingProcessor {

    private static final Map<String, String> CACHING_CONFIGURATIONS = Map.of(
            "quarkus.cache.caffeine.\"passphrase\".initial-capacity", "10000",
            "quarkus.cache.caffeine.\"passphrase\".maximum-size", "10000",
            "quarkus.cache.caffeine.\"passphrase\".expire-after-write", "7D");

    @BuildStep
    List<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItem(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return List.of(new AdditionalIndexedClassesBuildItem(CachedPassphraseRepository.class.getName()));
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeanBuildItems(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return List.of(AdditionalBeanBuildItem.builder().addBeanClass(CachedPassphraseRepository.class).build());
        } else {
            return List.of();
        }
    }

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
            contentBuildItems.add(new ContentBuildItem(new Title(Title.Level.SECOND, "Passphrase caching configuration")));
            contentBuildItems.add(new ContentBuildItem(CodeBlock.fromProperties(CACHING_CONFIGURATIONS)));
            return contentBuildItems;
        } else {
            return List.of();
        }
    }

}
