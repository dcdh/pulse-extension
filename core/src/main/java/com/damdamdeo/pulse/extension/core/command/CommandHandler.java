package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.saga.Saga;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class CommandHandler<A extends AggregateRoot<K>, K extends AggregateId> {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final EventRepository<A, K> eventRepository;
    private final Transaction transaction;
    private final ExecutionContextProvider executionContextProvider;
    private final List<Saga<K, Event<K>>> sagas;
    private final AggregateIdGenerator aggregateIdGenerator;

    public CommandHandler(final CommandHandlerRegistry commandHandlerRegistry,
                          final EventRepository<A, K> eventRepository,
                          final Transaction transaction,
                          final ExecutionContextProvider executionContextProvider,
                          final List<Saga<K, Event<K>>> sagas,
                          final AggregateIdGenerator aggregateIdGenerator) {
        this.commandHandlerRegistry = Objects.requireNonNull(commandHandlerRegistry);
        this.eventRepository = Objects.requireNonNull(eventRepository);
        this.transaction = Objects.requireNonNull(transaction);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.sagas = Objects.requireNonNull(sagas);
        this.aggregateIdGenerator = Objects.requireNonNull(aggregateIdGenerator);
    }

    public A handle(final Function<SequenceNumber, K> creational, final CreationalCommand<K> creationalCommand,
                    final Function<K, DuplicateAggregateException> duplicateAggregateExceptionSupplier) throws BusinessException, SequenceGenerationException {
        Objects.requireNonNull(creational);
        Objects.requireNonNull(creationalCommand);
        Objects.requireNonNull(duplicateAggregateExceptionSupplier);
        final ExecutionContext executionContext = executionContextProvider.provide();
        final K id = aggregateIdGenerator.generate(getAggregateIdClass(), creational);
        return commandHandlerRegistry.execute(id, () -> transaction.joiningExisting(() -> {
            if (eventRepository.hasEventsFor(id)) {
                throw new BusinessException(duplicateAggregateExceptionSupplier.apply(id));
            }
            final StateApplier<A, K> stateApplier = stateApplier(List.of(), id);
            final A aggregate = stateApplier.executeCommand(creationalCommand, executionContext);
            List<VersionizedEvent<K>> newEvents = stateApplier.getNewEvents();
            newEvents.forEach(newEvent -> sagas.forEach(saga -> saga.execute(id, newEvent.event())));
            eventRepository.save(newEvents, aggregate, executionContext.executedBy());
            return aggregate;
        }));
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
            final List<ExecutedByEvent<K>> events = eventRepository.loadOrderByVersionASC(command.id());
            if (events.isEmpty() && missingAggregateExceptionSupplier != null) {
                throw new BusinessException(missingAggregateExceptionSupplier.get());
            }
            final StateApplier<A, K> stateApplier = stateApplier(events, command.id());
            final A aggregate = stateApplier.executeCommand(command, executionContext);
            List<VersionizedEvent<K>> newEvents = stateApplier.getNewEvents();
            newEvents.forEach(newEvent -> sagas.forEach(saga -> saga.execute(command.id(), newEvent.event())));
            eventRepository.save(newEvents, aggregate, executionContext.executedBy());
            return aggregate;
        }));
    }

    abstract protected Class<A> getAggregateRootClass();

    abstract protected Class<K> getAggregateIdClass();

    private StateApplier<A, K> stateApplier(final List<ExecutedByEvent<K>> executedByEvents, final K aggregateId) {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(executedByEvents);
        return new StateApplier<>(new ReflectionAggregateRootInstanceCreator(), executionContextProvider,
                executedByEvents, getAggregateRootClass(), getAggregateIdClass(), aggregateId);
    }
}
