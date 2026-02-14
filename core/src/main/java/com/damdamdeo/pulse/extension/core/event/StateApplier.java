package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.Command;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public final class StateApplier<A extends AggregateRoot<K>, K extends AggregateId> implements EventAppender {

    private static final String COMMAND_HANDLER_METHOD_NAMING = "handle";
    private static final String EVENT_HANDLER_METHOD_NAMING = "on";

    private final A aggregate;
    private final List<VersionizedEvent> newEvents;
    private final Map<Class<Command<K>>, Method> cacheCommandHandlerMethods;
    private final Map<Class<Event>, Method> cacheEventMethods;
    private AggregateVersion aggregateVersion;

    public StateApplier(final AggregateRootInstanceCreator aggregateRootInstanceCreator,
                        final List<Event> events,
                        final Class<A> aggregateRootClass,
                        final Class<K> aggregateIdClass,
                        final K aggregateId) {
        Objects.requireNonNull(aggregateRootInstanceCreator);
        Objects.requireNonNull(events);
        Objects.requireNonNull(aggregateRootClass);
        Objects.requireNonNull(aggregateIdClass);
        Objects.requireNonNull(aggregateId);
        this.aggregate = aggregateRootInstanceCreator.create(aggregateRootClass, aggregateIdClass, aggregateId);
        // TODO will be created each time a StateApplier is created thus each time an Aggregate is created, loaded
        // This should be defined globally elsewhere
        this.cacheCommandHandlerMethods = Arrays.stream(aggregateRootClass.getDeclaredMethods())
                .filter(m -> COMMAND_HANDLER_METHOD_NAMING.equals(m.getName()))
                .filter(m -> m.getParameterCount() == 3)
                .filter(m -> Command.class.isAssignableFrom(m.getParameterTypes()[0]))
                .filter(m -> ExecutionContext.class.isAssignableFrom(m.getParameterTypes()[1]))
                .filter(m -> EventAppender.class.isAssignableFrom(m.getParameterTypes()[2]))
                .filter(m -> m.getExceptionTypes().length == 1)
                .filter(m -> m.getExceptionTypes()[0].equals(BusinessException.class))
                .filter(m -> m.canAccess(aggregate))
                .collect(Collectors.toMap(
                        m -> (Class<Command<K>>) m.getParameterTypes()[0],
                        m -> m,
                        (m1, m2) -> m1
                ));
        this.cacheEventMethods = Arrays.stream(aggregateRootClass.getDeclaredMethods())
                .filter(m -> EVENT_HANDLER_METHOD_NAMING.equals(m.getName()))
                .filter(m -> m.getParameterCount() == 1)
                .filter(m -> Event.class.isAssignableFrom(m.getParameterTypes()[0]))
                .filter(m -> m.canAccess(aggregate))
                .collect(Collectors.toMap(
                        m -> (Class<Event>) m.getParameterTypes()[0],
                        m -> m,
                        (m1, m2) -> m1
                ));
        events.forEach(this::apply);
        this.newEvents = new ArrayList<>();
        this.aggregateVersion = new AggregateVersion(events.size());
    }

    @Override
    public void append(final Event event) {
        Objects.requireNonNull(event);
        apply(event);
        this.newEvents.add(new VersionizedEvent(this.aggregateVersion, event));
        this.aggregateVersion = this.aggregateVersion.increment();
    }

    public A executeCommand(final Command<K> command, final ExecutionContext executionContext) throws BusinessException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(executionContext);
        try {
            if (!this.cacheCommandHandlerMethods.containsKey(command.getClass())) {
                throw new UnsupportedOperationException("Missing 'handle' method for command class - you must implement the method 'public void handle(final %s %s, final ExecutionContext executionContext, final EventAppender eventAppender) throws BusinessException' in '%s'"
                        .formatted(command.getClass().getSimpleName(), StringUtils.uncapitalize(command.getClass().getSimpleName()), aggregate.getClass().getSimpleName()));
            } else {
                this.cacheCommandHandlerMethods.get(command.getClass()).invoke(aggregate, command, executionContext, this);
            }
        } catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            } else if (cause instanceof UnsupportedOperationException unsupportedOperationException) {// FCKmrche pas
                throw unsupportedOperationException;
            } else {
                throw new RuntimeException("Error invoking event sourcing command handler", cause);
            }
        } catch (final IllegalAccessException e) {
            throw new RuntimeException("Cannot access event sourcing command handler method", e);
        }
        return this.aggregate;
    }

    private void apply(final Event event) {
        Objects.requireNonNull(event);
        if (!this.cacheEventMethods.containsKey(event.getClass())) {
            throw new UnsupportedOperationException("Missing 'on' method for event class - you must implement the method 'public void on(final %s %s)' in '%s'"
                    .formatted(event.getClass().getSimpleName(), StringUtils.uncapitalize(event.getClass().getSimpleName()), aggregate.getClass().getSimpleName()));
        } else {
            try {
                this.cacheEventMethods.get(event.getClass()).invoke(aggregate, event);
            } catch (final Exception e) {
                throw new RuntimeException("Error invoking event sourcing event handler", e);
            }
        }
    }

    A aggregate() {
        return aggregate;
    }

    public List<VersionizedEvent> getNewEvents() {
        return newEvents;
    }
}
