package com.damdamdeo.pulse.extension.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

abstract class StateApplier<A extends AggregateRoot<K>, K extends AggregateId<?>> implements EventAppender<K> {

    private static final String EVENT_HANDLER_METHOD_NAMING = "on";

    private final A aggregate;
    private final List<Event<K>> newEvents;

    public StateApplier(final AggregateRootInstanceCreator aggregateRootInstanceCreator,
                        final List<Event<K>> events) {
        Objects.requireNonNull(aggregateRootInstanceCreator);
        Objects.requireNonNull(events);
        this.aggregate = aggregateRootInstanceCreator.create(getAggregateClass());
        events.forEach(this::apply);
        this.newEvents = new ArrayList<>();
    }

    abstract Class<A> getAggregateClass();

    @Override
    public void append(final Event<K> event) {
        Objects.requireNonNull(event);
        apply(event);
        this.newEvents.add(event);
    }

    private void apply(final Event<K> event) {
        final K aggregateId = aggregate.id();
        if (aggregateId != null && !event.id().equals(aggregateId)) {
            throw new IllegalStateException("Applying event on an aggregate with different id.");
        }
        // TODO use a cache mechanism
        Arrays.stream(aggregate.getClass().getDeclaredMethods())
                .filter(m -> EVENT_HANDLER_METHOD_NAMING.equals(m.getName()))
                .filter(m -> m.getParameterCount() == 1)
                .filter(m -> m.getParameterTypes()[0].isAssignableFrom(event.getClass()))
                .filter(m -> m.canAccess(aggregate))
                .findFirst()
                .ifPresent(m -> {
                    try {
                        m.invoke(aggregate, event);
                    } catch (Exception e) {
                        throw new RuntimeException("Error invoking event sourcing handler", e);
                    }
                });
    }

    A aggregate() {
        return aggregate;
    }

    public List<Event<K>> getNewEvents() {
        return newEvents;
    }
}
