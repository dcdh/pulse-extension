package com.damdamdeo.pulse.extension.common.runtime.connectionidentifier;

import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifier;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepositoryException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.DuplicateConnectionIdentifierException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

@Unremovable
@Priority(1)
@Decorator
public class CachedConnectionIdentifierRepository implements ConnectionIdentifierRepository {

    final Logger LOGGER = Logger.getLogger(CachedConnectionIdentifierRepository.class.getName());

    @Inject
    @Any
    @Delegate
    ConnectionIdentifierRepository delegate;

    // https://github.com/quarkusio/quarkus/issues/19676
    @Inject
    @CacheName("connectionIdentifier")
    Cache cache;

    @Override
    public ConnectionIdentifier store(final ConnectionIdentifier connectionIdentifier, final Identifiable identifiable) throws ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException {
        Objects.requireNonNull(connectionIdentifier);
        Objects.requireNonNull(identifiable);
        final ConnectionIdentifier stored = delegate.store(connectionIdentifier, identifiable);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        caffeineCache.put(connectionIdentifier, CompletableFuture.completedFuture(identifiable));
        return stored;
    }

    @Override
    public Optional<Identifiable> find(final ConnectionIdentifier connectionIdentifier) throws ConnectionIdentifierRepositoryException {
        Objects.requireNonNull(connectionIdentifier);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        final CompletableFuture<AnyIdentifiable> findFromCache = caffeineCache.getIfPresent(connectionIdentifier);
        if (findFromCache == null) {
            final Optional<Identifiable> identifiable = delegate.find(connectionIdentifier);
            if (identifiable.isEmpty()) {
                return Optional.empty();
            }
            caffeineCache.put(connectionIdentifier, CompletableFuture.completedFuture(identifiable.get()));
            return identifiable;
        } else {
            try {
                return Optional.of(findFromCache.get());
            } catch (final InterruptedException | ExecutionException exception) {
                LOGGER.warning("Unable to find connectionIdentifier from cache - execute delegate " + exception.getMessage());
                return delegate.find(connectionIdentifier);
            }
        }
    }
}
