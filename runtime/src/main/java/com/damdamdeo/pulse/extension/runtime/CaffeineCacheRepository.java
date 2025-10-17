package com.damdamdeo.pulse.extension.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.event.CacheRepository;
import com.damdamdeo.pulse.extension.core.event.CachedAggregateRoot;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import jakarta.inject.Inject;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class CaffeineCacheRepository<A extends AggregateRoot<K>, K extends AggregateId> implements CacheRepository<A, K> {

    @Inject
    @CacheName("query-event-store")
    Cache cache;

    void toto() {
        cache.as(CaffeineCache.class).put("foo", CompletableFuture.completedFuture("bar"));
        cache.as(CaffeineCache.class).getIfPresent("TODO");
    }

    @Override
    public Optional<CachedAggregateRoot<A>> getById(final AggregateId aggregateId) {
        Objects.requireNonNull(aggregateId);
        return cache.as(CaffeineCache.class).get(aggregateId, (k) -> Optional.<CachedAggregateRoot<A>>empty())
                .await().indefinitely();
    }

    @Override
    public void store(final A aggregateRoot, final AggregateVersion aggregateVersion) {
        Objects.requireNonNull(aggregateRoot);
        Objects.requireNonNull(aggregateVersion);
        cache.as(CaffeineCache.class).put();putain comment put mais immediatement ???
        FCK
    }
}
