package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.Command;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public final class StateApplier<A extends AggregateRoot<K>, K extends AggregateId> implements EventAppender<K> {

    private static final String COMMAND_HANDLER_METHOD_NAMING = "handle";
    private static final String EVENT_HANDLER_METHOD_NAMING = "on";

    private final A aggregate;
    private final List<VersionizedEvent<K>> newEvents;
    private final Map<Class<Command<K>>, Method> cacheCommandHandlerMethods;
    private final Map<Class<Event<K>>, Method> cacheEventMethods;
    private AggregateVersion aggregateVersion;

    public StateApplier(final AggregateRootInstanceCreator aggregateRootInstanceCreator,
                        final List<Event<K>> events,
                        final Class<A> aggregateClass) {
        Objects.requireNonNull(aggregateRootInstanceCreator);
        Objects.requireNonNull(events);
        this.aggregate = aggregateRootInstanceCreator.create(aggregateClass);
        // TODO will be created each time a StateApplier is created thus each time an Aggregate is created, loaded
        // This should be defined globally elsewhere
        this.cacheCommandHandlerMethods = Arrays.stream(aggregateClass.getDeclaredMethods())
                .filter(m -> COMMAND_HANDLER_METHOD_NAMING.equals(m.getName()))
                .filter(m -> m.getParameterCount() == 2)
                .filter(m -> Command.class.isAssignableFrom(m.getParameterTypes()[0]))
                .filter(m -> EventAppender.class.isAssignableFrom(m.getParameterTypes()[1]))
                .filter(m -> m.canAccess(aggregate))
                .collect(Collectors.toMap(
                        m -> (Class<Command<K>>) m.getParameterTypes()[0],
                        m -> m,
                        (m1, m2) -> m1
                ));
        this.cacheEventMethods = Arrays.stream(aggregateClass.getDeclaredMethods())
                .filter(m -> EVENT_HANDLER_METHOD_NAMING.equals(m.getName()))
                .filter(m -> m.getParameterCount() == 1)
                .filter(m -> Event.class.isAssignableFrom(m.getParameterTypes()[0]))
                .filter(m -> m.canAccess(aggregate))
                .collect(Collectors.toMap(
                        m -> (Class<Event<K>>) m.getParameterTypes()[0],
                        m -> m,
                        (m1, m2) -> m1
                ));
        events.forEach(this::apply);
        this.newEvents = new ArrayList<>();
        this.aggregateVersion = new AggregateVersion(events.size());
    }

    @Override
    public void append(final Event<K> event) {
        Objects.requireNonNull(event);
        apply(event);
        this.newEvents.add(new VersionizedEvent<>(this.aggregateVersion, event));
        this.aggregateVersion = this.aggregateVersion.increment();
    }

    public A executeCommand(final Command<K> command) {
        Objects.requireNonNull(command);
        try {
            this.cacheCommandHandlerMethods.get(command.getClass()).invoke(aggregate, command, this);
        } catch (Exception e) {
            throw new RuntimeException("Error invoking event sourcing handler", e);
        }
        return this.aggregate;
    }

    private void apply(final Event<K> event) {
        final K aggregateId = aggregate.id();
        if (aggregateId != null && !event.id().equals(aggregateId)) {
            throw new IllegalStateException("Applying event on an aggregate with different id.");
        }
        if (this.cacheEventMethods.containsKey(event.getClass())) {
            try {
                this.cacheEventMethods.get(event.getClass()).invoke(aggregate, event);
            } catch (Exception e) {
                throw new RuntimeException("Error invoking event sourcing handler", e);
            }
        }
    }

    A aggregate() {
        return aggregate;
    }

    public List<VersionizedEvent<K>> getNewEvents() {
        return newEvents;
    }
}
