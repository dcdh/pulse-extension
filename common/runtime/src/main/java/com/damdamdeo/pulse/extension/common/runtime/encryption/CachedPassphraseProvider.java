package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.CachedPassphraseRepository;
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

@Unremovable
@Priority(1)
@Decorator
public class CachedPassphraseProvider implements PassphraseProvider {

    @Inject
    @Any
    @Delegate
    PassphraseProvider delegate;

    @Inject
    @CacheName(CachedPassphraseRepository.CACHE_NAME)
    Cache cache;

    @Override
    public Passphrase provide(final OwnedBy ownedBy) throws UnableToProvidePassphraseException {
        Objects.requireNonNull(ownedBy);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        final CompletableFuture<RetrievedPassphrase> findFromCache = caffeineCache.getIfPresent(ownedBy);
        if (findFromCache == null) {
            // The delegate will populate the cache
            return delegate.provide(ownedBy);
        } else {
            try {
                return findFromCache.get().passphrase();
            } catch (final InterruptedException | ExecutionException exception) {
                throw new UnableToProvidePassphraseException(exception);
            }
        }
    }

    @Override
    public Passphrase ban(final OwnedBy ownedBy) throws UnableToBanPassphraseException {
        Objects.requireNonNull(ownedBy);
        // nothing to do regarding caching. Will be done in delegate methode.
        return delegate.ban(ownedBy);
    }
}
