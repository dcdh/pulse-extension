package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class QueryEventStore<A extends AggregateRoot<K>, K extends AggregateId> {

    private final EventRepository<A, K> eventStore;
    private final ExecutionContextProvider executionContextProvider;

    protected QueryEventStore(final EventRepository<A, K> eventStore,
                              final ExecutionContextProvider executionContextProvider) {
        this.eventStore = Objects.requireNonNull(eventStore);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
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
                    List<ExecutedByEvent> executedByEvents = eventStore.loadOrderByVersionASC(id, aggregateVersion);
                    if (executedByEvents.isEmpty()) {
                        return Optional.empty();
                    } else {
                        return Optional.of(stateApplier(executedByEvents, id).aggregate());
                    }
                });
    }

    abstract protected Class<A> getAggregateRootClass();

    abstract protected Class<K> getAggregateIdClass();

    private StateApplier<A, K> stateApplier(final List<ExecutedByEvent> executedByEvents, final K aggregateId) {
        Objects.requireNonNull(executedByEvents);
        Objects.requireNonNull(aggregateId);
        return new StateApplier<>(new ReflectionAggregateRootInstanceCreator(), executionContextProvider,
                executedByEvents, getAggregateRootClass(), getAggregateIdClass(), aggregateId);
    }
}
