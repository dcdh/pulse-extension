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

@Unremovable
@Priority(1)
@Decorator
public class CachedConnectionIdentifierRepository implements ConnectionIdentifierRepository {

    @Inject
    @Any
    @Delegate
    ConnectionIdentifierRepository delegate;

    @Inject
    @CacheName("connectionIdentifier")
    Cache cache;

    @Override
    public ConnectionIdentifier store(final ConnectionIdentifier connectionIdentifier, final Identifiable identifiable) throws ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException {
        Objects.requireNonNull(connectionIdentifier);
        Objects.requireNonNull(identifiable);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        caffeineCache.put(connectionIdentifier.id(), CompletableFuture.completedFuture(identifiable));
        return delegate.store(connectionIdentifier, identifiable);
    }

    @Override
    public Optional<Identifiable> find(final ConnectionIdentifier connectionIdentifier) throws ConnectionIdentifierRepositoryException {
        Objects.requireNonNull(connectionIdentifier);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        final CompletableFuture<AnyIdentifiable> findFromCache = caffeineCache.getIfPresent(connectionIdentifier.id());
        if (findFromCache == null) {
            final Optional<Identifiable> identifiable = delegate.find(connectionIdentifier);
            if (identifiable.isEmpty()) {
                return Optional.empty();
            }
            caffeineCache.put(connectionIdentifier.id(), CompletableFuture.completedFuture(identifiable.get()));
            return identifiable;
        } else {
            try {
                return Optional.of(findFromCache.get());
            } catch (final InterruptedException | ExecutionException exception) {
                throw new ConnectionIdentifierRepositoryException(exception);
            }
        }
    }
}
