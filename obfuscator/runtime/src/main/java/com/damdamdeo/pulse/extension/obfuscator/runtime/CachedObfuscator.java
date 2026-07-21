package com.damdamdeo.pulse.extension.obfuscator.runtime;

import com.damdamdeo.pulse.extension.core.obfuscator.Obfuscator;
import com.damdamdeo.pulse.extension.core.obfuscator.UnableToDeObfuscateException;
import com.damdamdeo.pulse.extension.core.obfuscator.UnableToObfuscateException;
import com.damdamdeo.pulse.extension.core.obfuscator.UnknownObfuscatedException;
import io.quarkus.arc.Unremovable;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

@Unremovable
@Priority(1)
@Decorator
public class CachedObfuscator implements Obfuscator {

    final Logger LOGGER = Logger.getLogger(CachedObfuscator.class.getName());

    @Inject
    @Any
    @Delegate
    Obfuscator delegate;

    @Inject
    @CacheName("obfuscator")
    Cache cache;

    @Override
    public String obfuscate(final String value) throws UnableToObfuscateException {
        Objects.requireNonNull(value);
        final String obfuscated = delegate.obfuscate(value);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        caffeineCache.put(obfuscated, CompletableFuture.completedFuture(value));
        return obfuscated;
    }

    @Override
    public String deObfuscate(final String obfuscated) throws UnableToDeObfuscateException, UnknownObfuscatedException {
        Objects.requireNonNull(obfuscated);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        final CompletableFuture<String> deObfuscatedFromCache = caffeineCache.getIfPresent(obfuscated);
        if (deObfuscatedFromCache == null) {
            final String deObfuscate = delegate.deObfuscate(obfuscated);
            caffeineCache.put(obfuscated, CompletableFuture.completedFuture(deObfuscate));
            return deObfuscate;
        } else {
            try {
                return deObfuscatedFromCache.get();
            } catch (final InterruptedException | ExecutionException exception) {
                LOGGER.warning("Unable to deobfuscate from cache - execute delegate " + exception.getMessage());
                return delegate.deObfuscate(obfuscated);
            }
        }
    }
}
