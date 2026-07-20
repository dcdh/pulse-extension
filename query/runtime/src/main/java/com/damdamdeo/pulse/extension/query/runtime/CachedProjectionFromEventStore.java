package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.*;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CaffeineCache;
import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class CachedProjectionFromEventStore<P extends Projection> implements ProjectionFromEventStore<P> {

    private final ProjectionFromEventStore<P> delegate;
    private final EventCounter eventCounter;

    private final Cache cache;

    public CachedProjectionFromEventStore(final ProjectionFromEventStore<P> delegate,
                                          final EventCounter eventCounter,
                                          final Cache cache) {
        this.delegate = Objects.requireNonNull(delegate);
        this.eventCounter = Objects.requireNonNull(eventCounter);
        this.cache = Objects.requireNonNull(cache);
    }

    @Override
    public Result<P> getOneByAggregateId(final AggregateId aggregateId,
                                         final SingleResultAggregateIdProjectionQuery singleResultAggregateIdProjectionQuery) throws ProjectionException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(singleResultAggregateIdProjectionQuery);
        try {
            final CaffeineCache caffeineCache = this.cache.as(CaffeineCache.class);
            final CompletableFuture<CachedValue<P>> findFromCache = caffeineCache.getIfPresent(aggregateId);
            if (findFromCache == null) {
                final Result<P> oneByAggregateId = this.delegate.getOneByAggregateId(aggregateId, singleResultAggregateIdProjectionQuery);
                final Integer count = eventCounter.byAggregateId(aggregateId);
                caffeineCache.put(aggregateId, CompletableFuture.completedFuture(new CachedValue<>(count, oneByAggregateId)));
                return oneByAggregateId;
            } else {
                try {
                    final CachedValue<P> cachedValue = findFromCache.get();
                    final Integer count = eventCounter.byAggregateId(aggregateId);
                    if (count > cachedValue.count()) {
                        final Result<P> oneByAggregateId = this.delegate.getOneByAggregateId(aggregateId, singleResultAggregateIdProjectionQuery);
                        caffeineCache.put(aggregateId, CompletableFuture.completedFuture(new CachedValue<>(count, oneByAggregateId)));
                        return oneByAggregateId;
                    } else {
                        return cachedValue.result();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new ProjectionException(aggregateId, e);
                }
            }
        } catch (final EventCounterException e) {
            throw new ProjectionException(aggregateId, e);
        }
    }

    @Override
    public Optional<Result<P>> findOneByAggregateId(final AggregateId aggregateId,
                                                    final SingleResultAggregateIdProjectionQuery singleResultAggregateIdProjectionQuery) throws ProjectionException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(singleResultAggregateIdProjectionQuery);
        try {
            final CaffeineCache caffeineCache = this.cache.as(CaffeineCache.class);
            final CompletableFuture<CachedValue<P>> findFromCache = caffeineCache.getIfPresent(aggregateId);
            if (findFromCache == null) {
                final Optional<Result<P>> oneByAggregateId = this.delegate.findOneByAggregateId(aggregateId, singleResultAggregateIdProjectionQuery);
                if (oneByAggregateId.isPresent()) {
                    final Integer count = eventCounter.byAggregateId(aggregateId);
                    caffeineCache.put(aggregateId, CompletableFuture.completedFuture(new CachedValue<>(count, oneByAggregateId.get())));
                }
                return oneByAggregateId;
            } else {
                try {
                    final CachedValue<P> cachedValue = findFromCache.get();
                    final Integer count = eventCounter.byAggregateId(aggregateId);
                    if (count > cachedValue.count()) {
                        final Optional<Result<P>> oneByAggregateId = this.delegate.findOneByAggregateId(aggregateId, singleResultAggregateIdProjectionQuery);
                        oneByAggregateId.ifPresent(result -> caffeineCache.put(aggregateId, CompletableFuture.completedFuture(new CachedValue<>(count, result))));
                        return oneByAggregateId;
                    } else {
                        return Optional.of(cachedValue.result());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new ProjectionException(aggregateId, e);
                }
            }
        } catch (final EventCounterException e) {
            throw new ProjectionException(aggregateId, e);
        }
    }

    @Override
    public <I extends Input> Result<P> findAllBy(final OwnedBy ownedBy,
                                                 final I input,
                                                 final MultipleResultProjectionQuery<I> multipleResultProjectionQuery) throws ProjectionException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(input);
        Objects.requireNonNull(multipleResultProjectionQuery);
        try {
            final CaffeineCache caffeineCache = this.cache.as(CaffeineCache.class);
            final CompletableFuture<CachedValue<P>> findFromCache = caffeineCache.getIfPresent(new Key<>(ownedBy, input));
            if (findFromCache == null) {
                final Result<P> oneByAggregateId = this.delegate.findAllBy(ownedBy, input, multipleResultProjectionQuery);
                final Integer count = eventCounter.byOwnedBy(ownedBy);
                caffeineCache.put(new Key<>(ownedBy, input), CompletableFuture.completedFuture(new CachedValue<>(count, oneByAggregateId)));
                return oneByAggregateId;
            } else {
                try {
                    final CachedValue<P> cachedValue = findFromCache.get();
                    final Integer count = eventCounter.byOwnedBy(ownedBy);
                    if (count > cachedValue.count()) {
                        final Result<P> oneByAggregateId = this.delegate.findAllBy(ownedBy, input, multipleResultProjectionQuery);
                        caffeineCache.put(new Key<>(ownedBy, input), CompletableFuture.completedFuture(new CachedValue<>(count, oneByAggregateId)));
                        return oneByAggregateId;
                    } else {
                        return cachedValue.result();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new ProjectionException(ownedBy, e);
                }
            }
        } catch (final EventCounterException e) {
            throw new ProjectionException(ownedBy, e);
        }
    }

    record Key<I extends Input>(OwnedBy ownedBy, I input) {

        Key {
            Objects.requireNonNull(ownedBy);
            Objects.requireNonNull(input);
        }
    }

    record CachedValue<P extends Projection>(Integer count, Result<P> result) {

        CachedValue {
            Objects.requireNonNull(count);
            Objects.requireNonNull(result);
            Validate.isTrue(count > 0);
        }
    }
}
