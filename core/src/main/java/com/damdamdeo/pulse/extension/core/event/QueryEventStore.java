package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class QueryEventStore<A extends AggregateRoot<K>, K extends AggregateId> {

    private final EventRepository<A, K> eventStore;

    protected QueryEventStore(final EventRepository<A, K> eventStore) {
        this.eventStore = Objects.requireNonNull(eventStore);
    }

    public Optional<A> findById(final K id) {
        return eventStore.findLastVersionById(id)
                .map(VersionizedAggregateRoot::aggregateRoot);
    }

    public Optional<A> findByIdAndVersion(final K id, final AggregateVersion aggregateVersion) {
        return eventStore.findLastVersionById(id)
                .filter(lastVersionById -> lastVersionById.aggregateVersion().equals(aggregateVersion))
                .map(VersionizedAggregateRoot::aggregateRoot)
                .or(() -> {
                    List<Event<K>> events = eventStore.loadOrderByVersionASC(id, aggregateVersion);
                    if (events.isEmpty()) {
                        return Optional.empty();
                    } else {
                        return Optional.of(stateApplier(events).aggregate());
                    }
                });
    }

    abstract protected Class<A> getAggregateClass();

    private StateApplier<A, K> stateApplier(List<Event<K>> events) {
        return new StateApplier<>(new ReflectionAggregateRootInstanceCreator(), events, getAggregateClass());
    }
}
