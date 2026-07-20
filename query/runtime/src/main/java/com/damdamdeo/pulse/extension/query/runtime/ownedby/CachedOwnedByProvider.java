package com.damdamdeo.pulse.extension.query.runtime.ownedby;

import com.damdamdeo.pulse.extension.core.AggregateId;
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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Unremovable
@Priority(1)
@Decorator
public class CachedOwnedByProvider implements OwnedByProvider {

    @Inject
    @Any
    @Delegate
    OwnedByProvider delegate;

    @Inject
    @CacheName("ownedBy")
    Cache cache;

    @Override
    public OwnedBy getByAggregateId(final AggregateId aggregateId) throws UnableToProvideOwnedByException {
        Objects.requireNonNull(aggregateId);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        final CompletableFuture<OwnedBy> ownedByFromCache = caffeineCache.getIfPresent(aggregateId);
        if (ownedByFromCache == null) {
            final OwnedBy retrieved = delegate.getByAggregateId(aggregateId);
            caffeineCache.put(aggregateId, CompletableFuture.completedFuture(retrieved));
            return retrieved;
        } else {
            try {
                return ownedByFromCache.get();
            } catch (final InterruptedException | ExecutionException exception) {
                throw new UnableToProvideOwnedByException(exception);
            }
        }
    }
}
