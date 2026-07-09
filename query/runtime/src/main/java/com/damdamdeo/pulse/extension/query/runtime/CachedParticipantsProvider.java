package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.ParticipantsProvider;
import io.quarkus.arc.Unremovable;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Unremovable
@Priority(1)
@Decorator
public class CachedParticipantsProvider implements ParticipantsProvider {

    @Inject
    @Any
    @Delegate
    ParticipantsProvider delegate;

    @Inject
    @CacheName("participant")
    Cache cache;

    @Override
    public Set<ExecutedBy> findParticipants(final Set<AggregateId> aggregatesId) {
        Objects.requireNonNull(aggregatesId);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        final List<AggregateId> missing = new ArrayList<>(aggregatesId.size());
        final Set<ExecutedBy> executedBys = new HashSet<>();
        for (final AggregateId aggregateId : aggregatesId) {
            final CompletableFuture<List<ExecutedBy>> participantsFromCache = caffeineCache.getIfPresent(aggregateId);
            if (participantsFromCache == null) {
                missing.add(aggregateId);
            } else {
                executedBys.addAll(participantsFromCache.join());
            }
        }
        if (missing.isEmpty()) {
            return executedBys;
        } else {
            this.findParticipantRelations(missing).values().forEach(executedBys::addAll);
        }
        return executedBys;
    }

    @Override
    public Map<ExecutedBy, Set<ExecutedBy>> findParticipantRelations(final Set<AggregateId> aggregateIds) {
        Objects.requireNonNull(aggregateIds);
        final Map<ExecutedBy, List<ExecutedBy>> participantRelations = this.delegate.findParticipantRelations(aggregateIds);
        final CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        participantRelations.forEach((executedBy, executedBys) ->
                caffeineCache.put(executedBy, CompletableFuture.completedFuture(executedBys)));
        return participantRelations;
    }

    void onNewEvent(@Observes final NewEvent newEvent) {
        cache.invalidate().await().indefinitely();
    }
}
