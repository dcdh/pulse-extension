package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import com.damdamdeo.pulse.extension.query.runtime.JdbcEventCounter;
import com.damdamdeo.pulse.extension.query.runtime.file.CachedFileRepository;
import com.damdamdeo.pulse.extension.query.runtime.file.FileCacheCleaner;
import com.damdamdeo.pulse.extension.query.runtime.file.FileCacheProducer;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.CachedOwnedByProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.cache.deployment.spi.AdditionalCacheNameBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CachingProcessor {

    private static final String CACHE_NAME = "projection";

    private static final Map<String, String> CACHING_CONFIGURATIONS = Map.of(
            "quarkus.cache.caffeine.\"projection\".initial-capacity", "10000",
            "quarkus.cache.caffeine.\"projection\".maximum-size", "10000",
            "quarkus.cache.caffeine.\"projection\".expire-after-write", "7D",
            "quarkus.cache.caffeine.\"ownedBy\".initial-capacity", "10000",
            "quarkus.cache.caffeine.\"ownedBy\".maximum-size", "10000",
            "quarkus.cache.caffeine.\"ownedBy\".expire-after-write", "7D",
            "quarkus.cache.caffeine.\"file\".initial-capacity", "10000",
            "quarkus.cache.caffeine.\"file\".maximum-size", "10000",
            "quarkus.cache.caffeine.\"file\".expire-after-write", "7D",
            "pulse.query.file.cleanup.every", "5m");

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
            contentBuildItems.add(new ContentBuildItem(new Title(Title.Level.SECOND, "Projection caching configuration")));
            contentBuildItems.add(new ContentBuildItem(CodeBlock.fromProperties(CACHING_CONFIGURATIONS)));
            return contentBuildItems;
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItem(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return List.of(new AdditionalIndexedClassesBuildItem(CachedOwnedByProvider.class.getName(),
                    CachedFileRepository.class.getName(),
                    FileCacheCleaner.class.getName(),
                    FileCacheProducer.class.getName()));
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeanBuildItems(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return List.of(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(CachedOwnedByProvider.class,
                            CachedFileRepository.class,
                            FileCacheCleaner.class,
                            FileCacheProducer.class,
                            JdbcEventCounter.class)
                    .build());
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalCacheNameBuildItem> registerProjectionCache(final Capabilities capabilities) {
        if (capabilities.isPresent(Capability.CACHE)) {
            return List.of(new AdditionalCacheNameBuildItem(CACHE_NAME));
        } else {
            return List.of();
        }
    }
}
