package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

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
        return execute(command, executionContextProvider.provide(), null);
    }

    public A handle(final Command<K> command, final Supplier<MissingAggregateException> missingAggregateExceptionSupplier) throws BusinessException {
        return execute(command, executionContextProvider.provide(), missingAggregateExceptionSupplier);
    }

    private A execute(final Command<K> command, final ExecutionContext executionContext,
                      final Supplier<MissingAggregateException> missingAggregateExceptionSupplier) throws BusinessException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(executionContext);
        return commandHandlerRegistry.execute(command.id(), () -> transaction.joiningExisting(() -> {
            final List<ExecutedByEvent> events = eventRepository.loadOrderByVersionASC(command.id());
            if (events.isEmpty() && missingAggregateExceptionSupplier != null) {
                throw new BusinessException(missingAggregateExceptionSupplier.get());
            }
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

    private StateApplier<A, K> stateApplier(final List<ExecutedByEvent> executedByEvents, final K aggregateId) {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(executedByEvents);
        return new StateApplier<>(new ReflectionAggregateRootInstanceCreator(), executionContextProvider,
                executedByEvents, getAggregateRootClass(), getAggregateIdClass(), aggregateId);
    }
}
