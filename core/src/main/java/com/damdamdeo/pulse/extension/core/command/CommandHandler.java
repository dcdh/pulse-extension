package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;

import java.util.List;
import java.util.Objects;

public abstract class CommandHandler<A extends AggregateRoot<K>, K extends AggregateId> {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final EventRepository<A, K> eventRepository;
    private final Transaction transaction;
    private final ExecutionContextProvider executionContextProvider;
    private final EventNotifier eventNotifier;

    public CommandHandler(final CommandHandlerRegistry commandHandlerRegistry,
                          final EventRepository<A, K> eventRepository,
                          final Transaction transaction,
                          final ExecutionContextProvider executionContextProvider,
                          final EventNotifier eventNotifier) {
        this.commandHandlerRegistry = Objects.requireNonNull(commandHandlerRegistry);
        this.eventRepository = Objects.requireNonNull(eventRepository);
        this.transaction = Objects.requireNonNull(transaction);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.eventNotifier = Objects.requireNonNull(eventNotifier);
    }

    public A handle(final Command<K> command) throws BusinessException {
        return execute(command, executionContextProvider.provide());
    }

    private A execute(final Command<K> command, final ExecutionContext executionContext) throws BusinessException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(executionContext);
        return commandHandlerRegistry.execute(command.id(), () -> transaction.joiningExisting(() -> {
            final List<Event> events = eventRepository.loadOrderByVersionASC(command.id());
            final StateApplier<A, K> stateApplier = stateApplier(events, command.id());
            final A aggregate = stateApplier.executeCommand(command, executionContext);
            List<VersionizedEvent> newEvents = stateApplier.getNewEvents();
            newEvents.forEach(newEvent -> eventNotifier.notify(
                    new IdentifiableEvent(command.id().id(), newEvent.event())));
            eventRepository.save(newEvents, aggregate, executionContext.executedBy());
            return aggregate;
        }));
    }

    abstract protected Class<A> getAggregateRootClass();

    abstract protected Class<K> getAggregateIdClass();

    private StateApplier<A, K> stateApplier(final List<Event> events, final K aggregateId) {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(events);
        return new StateApplier<>(new ReflectionAggregateRootInstanceCreator(), events, getAggregateRootClass(),
                getAggregateIdClass(), aggregateId);
    }
}
