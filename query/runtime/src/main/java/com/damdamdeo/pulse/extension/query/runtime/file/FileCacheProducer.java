package com.damdamdeo.pulse.extension.query.runtime.file;

import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.quarkus.cache.runtime.CacheConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.eclipse.microprofile.context.ManagedExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

@ApplicationScoped
public class FileCacheProducer {

    static final Logger LOGGER = Logger.getLogger(FileCacheProducer.class.getName());

    public static final String CACHE_NAME = "file";

    @Produces
    @ApplicationScoped
    @Named(CACHE_NAME)
    Cache<FileIdentifier, CachedFileRepository.FileCache> cache(final CacheConfig cacheConfig, final ManagedExecutor managedExecutor) {
        final CacheConfig.CaffeineConfig.CaffeineCacheConfig config = cacheConfig.caffeine().cachesConfig().get(CACHE_NAME);
        final Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder();
        config.initialCapacity().ifPresent(caffeineBuilder::initialCapacity);
        config.maximumSize().ifPresent(caffeineBuilder::maximumSize);
        config.expireAfterWrite().ifPresent(caffeineBuilder::expireAfterWrite);
        config.expireAfterAccess().ifPresent(caffeineBuilder::expireAfterAccess);
        caffeineBuilder.executor(managedExecutor);
        caffeineBuilder.removalListener((final FileIdentifier key, final CachedFileRepository.FileCache value, final RemovalCause cause) -> {
            try {
                Files.deleteIfExists(value.path());
            } catch (final IOException exception) {
                LOGGER.warning("Unable to delete file: " + value.path() + " cause: " + exception.getMessage());
            }
        });
        return caffeineBuilder.build();
    }
}
