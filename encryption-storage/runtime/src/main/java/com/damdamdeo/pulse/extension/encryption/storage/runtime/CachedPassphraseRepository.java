package com.damdamdeo.pulse.extension.encryption.storage.runtime;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import io.quarkus.arc.Unremovable;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

@Unremovable
@Priority(1)
@Decorator
public class CachedPassphraseRepository implements PassphraseRepository {

    final Logger LOGGER = Logger.getLogger(CachedPassphraseRepository.class.getName());

    public static final String CACHE_NAME = "passphrase";

    @Inject
    @Any
    @Delegate
    PassphraseRepository delegate;

    @Inject
    @CacheName(CACHE_NAME)
    Cache cache;

    @Override
    public Optional<Passphrase> findBy(final OwnedBy ownedBy) throws UnableToRetrievePassphraseException {
        Objects.requireNonNull(ownedBy);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        final CompletableFuture<RetrievedPassphrase> findFromCache = caffeineCache.getIfPresent(ownedBy);
        if (findFromCache == null) {
            final Optional<Passphrase> found = delegate.findBy(ownedBy);
            if (found.isPresent()) {
                caffeineCache.put(ownedBy, CompletableFuture.completedFuture(new RetrievedPassphrase(ownedBy, found.get())));
            } else {
                caffeineCache.put(ownedBy, CompletableFuture.completedFuture(new RetrievedPassphrase(ownedBy, null)));
            }
            return found;
        } else {
            try {
                return findFromCache.get().passphraseAsOptional();
            } catch (final InterruptedException | ExecutionException exception) {
                LOGGER.warning("Unable to find by from cache - execute delegate " + exception.getMessage());
                return delegate.findBy(ownedBy);
            }
        }
    }

    @Override
    public Passphrase get(final OwnedBy ownedBy) throws UnableToRetrievePassphraseException, UnknownPassphraseException {
        Objects.requireNonNull(ownedBy);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        final CompletableFuture<RetrievedPassphrase> findFromCache = caffeineCache.getIfPresent(ownedBy);
        if (findFromCache == null) {
            final Passphrase passphrase = delegate.get(ownedBy);
            caffeineCache.put(ownedBy, CompletableFuture.completedFuture(new RetrievedPassphrase(ownedBy, passphrase)));
            return passphrase;
        } else {
            try {
                return findFromCache.get().passphrase();
            } catch (final InterruptedException | ExecutionException exception) {
                LOGGER.warning("Unable to get from cache - execute delegate " + exception.getMessage());
                return delegate.get(ownedBy);
            }
        }
    }

    @Override
    public List<RetrievedPassphrase> list(final List<OwnedBy> multiples) throws UnableToRetrievePassphraseException {
        Objects.requireNonNull(multiples);
        try {
            final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
            final List<OwnedBy> missing = new ArrayList<>(multiples.size());
            final List<RetrievedPassphrase> retrievedPassphrases = new ArrayList<>(multiples.size());
            for (final OwnedBy ownedBy : multiples) {
                final CompletableFuture<RetrievedPassphrase> findFromCache = caffeineCache.getIfPresent(ownedBy);
                if (findFromCache == null) {
                    missing.add(ownedBy);
                } else {
                    retrievedPassphrases.add(findFromCache.get());
                }
            }
            if (!missing.isEmpty()) {
                final List<RetrievedPassphrase> retrievedPassphrasesFromDelegate = delegate.list(missing);
                retrievedPassphrasesFromDelegate.forEach(retrievedPassphraseFromDelegate -> caffeineCache.put(retrievedPassphraseFromDelegate.ownedBy(),
                        CompletableFuture.completedFuture(retrievedPassphraseFromDelegate)));
                retrievedPassphrases.addAll(retrievedPassphrasesFromDelegate);
            }
            return retrievedPassphrases;
        } catch (final InterruptedException | ExecutionException exception) {
            LOGGER.warning("Unable to list from cache - execute delegate " + exception.getMessage());
            return delegate.list(multiples);
        }
    }

    @Override
    public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(passphrase);
        final Passphrase stored = delegate.store(ownedBy, passphrase);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        caffeineCache.put(ownedBy, CompletableFuture.completedFuture(new RetrievedPassphrase(ownedBy, passphrase)));
        return stored;
    }

    @Override
    public Passphrase update(final OwnedBy ownedBy, final Passphrase passphrase) throws UnableToStorePassphraseException, UnknownPassphraseException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(passphrase);
        final Passphrase updated = delegate.update(ownedBy, passphrase);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        caffeineCache.put(ownedBy, CompletableFuture.completedFuture(new RetrievedPassphrase(ownedBy, passphrase)));
        return updated;
    }
}
