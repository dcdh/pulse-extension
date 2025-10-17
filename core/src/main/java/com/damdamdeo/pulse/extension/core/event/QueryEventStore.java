package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.ReflectionAggregateRootInstanceCreator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class QueryEventStore<A extends AggregateRoot<K>, K extends AggregateId> {

    private final EventRepository<A, K> eventStore;
    private final CacheRepository<A, K> cacheRepository;

    protected QueryEventStore(final EventRepository<A, K> eventStore,
                              final CacheRepository<A, K> cacheRepository) {
        this.eventStore = Objects.requireNonNull(eventStore);
        this.cacheRepository = Objects.requireNonNull(cacheRepository);
    }

    public Optional<A> findById(final K id) {
        final Optional<AggregateVersion> lastVersion = eventStore.getLastVersionByAggregateId(id);
        if (lastVersion.isEmpty()) {
            return Optional.empty();
        }

        final AggregateVersion version = lastVersion.get();
        final Optional<A> cached = cacheRepository.getById(id)
                .filter(c -> c.aggregateVersion().equals(version))
                .map(CachedAggregateRoot::aggregateRoot);

        if (cached.isPresent()) {
            return cached;
        }

        final List<Event<K>> events = eventStore.loadOrderByVersionASC(id);
        if (events.isEmpty()) {
            throw new IllegalStateException("Inconsistent state: version exists but no events for id " + id);
        }

        final A aggregate = stateApplier(events).aggregate();
        cacheRepository.store(aggregate, version);
        return Optional.of(aggregate);
    }

    abstract protected Class<A> getAggregateClass();

    private StateApplier<A, K> stateApplier(List<Event<K>> events) {
        return new StateApplier<>(new ReflectionAggregateRootInstanceCreator(), events, getAggregateClass());
    }
}
