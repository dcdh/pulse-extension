package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.saga.OnStoredEventListener;
import com.damdamdeo.pulse.extension.core.saga.OnStoredEventListenerException;
import com.damdamdeo.pulse.extension.core.traceability.From;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppender;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppenderException;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class CommandHandler<A extends AggregateRoot<K>, K extends AggregateId> {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final EventRepository<A, K> eventRepository;
    private final Transaction transaction;
    private final ExecutionContextProvider executionContextProvider;
    private final List<OnStoredEventListener<K, Event<K>>> onStoredEventListeners;
    private final AggregateIdGenerator aggregateIdGenerator;
    private final TraceAppender traceAppender;

    public CommandHandler(final CommandHandlerRegistry commandHandlerRegistry,
                          final EventRepository<A, K> eventRepository,
                          final Transaction transaction,
                          final ExecutionContextProvider executionContextProvider,
                          final List<OnStoredEventListener<K, Event<K>>> onStoredEventListeners,
                          final AggregateIdGenerator aggregateIdGenerator,
                          final TraceAppender traceAppender) {
        this.commandHandlerRegistry = Objects.requireNonNull(commandHandlerRegistry);
        this.eventRepository = Objects.requireNonNull(eventRepository);
        this.transaction = Objects.requireNonNull(transaction);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.onStoredEventListeners = Objects.requireNonNull(onStoredEventListeners);
        this.aggregateIdGenerator = Objects.requireNonNull(aggregateIdGenerator);
        this.traceAppender = Objects.requireNonNull(traceAppender);
    }

    public final A handle(final K id, final CreationalCommand<K> creationalCommand,
                          final Function<K, DuplicateAggregateException> duplicateAggregateExceptionSupplier) throws CommandException {
        Objects.requireNonNull(id);
        Objects.requireNonNull(creationalCommand);
        Objects.requireNonNull(duplicateAggregateExceptionSupplier);
        final ExecutionContext executionContext = executionContextProvider.provide();
        Validate.validState(!executionContext.executedBy().value().equals(ExecutedBy.Banned.DISCRIMINANT));
        return transaction.joiningExisting(() -> {
            try {
                if (eventRepository.hasEventsFor(id)) {
                    throw duplicateAggregateExceptionSupplier.apply(id);
                }
                final StateApplier<A, K> stateApplier = stateApplier(List.of(), id);
                final A aggregate = commandHandlerRegistry.execute(id,
                        () -> stateApplier.executeCommand(creationalCommand, executionContext));
                final List<VersionizedEvent<K>> newEvents = stateApplier.getNewEvents();
                for (final VersionizedEvent<K> newEvent : newEvents) {
                    for (final OnStoredEventListener<K, Event<K>> onStoredEventListener : onStoredEventListeners) {
                        onStoredEventListener.execute(id, newEvent.event());
                    }
                }
                eventRepository.save(newEvents, aggregate, executionContext.executedBy());
                traceAppender.append(new AggregateIdTraceable(aggregate.id()), From.from(creationalCommand));
                return aggregate;
            } catch (final DuplicateAggregateException | BusinessException | OnStoredEventListenerException |
                           TraceAppenderException exception) {
                throw new CommandException(exception);
            }
        });
    }

    public final A handle(final Function<SequenceNumber, K> creational, final CreationalCommand<K> creationalCommand,
                          final Function<K, DuplicateAggregateException> duplicateAggregateExceptionSupplier) throws CommandException {
        Objects.requireNonNull(creational);
        Objects.requireNonNull(creationalCommand);
        Objects.requireNonNull(duplicateAggregateExceptionSupplier);
        final ExecutionContext executionContext = executionContextProvider.provide();
        Validate.validState(!executionContext.executedBy().value().equals(ExecutedBy.Banned.DISCRIMINANT));
        return transaction.joiningExisting(() -> {
            try {
                final K id;
                if (creationalCommand.belongsTo().isPresent()) {
                    id = aggregateIdGenerator.generate(new For<>(getAggregateIdClass(), creationalCommand.belongsTo().get()), creational);
                } else {
                    id = aggregateIdGenerator.generate(getAggregateIdClass(), creational);
                }
                if (eventRepository.hasEventsFor(id)) {
                    throw duplicateAggregateExceptionSupplier.apply(id);
                }
                final StateApplier<A, K> stateApplier = stateApplier(List.of(), id);
                final A aggregate = commandHandlerRegistry.execute(id,
                        () -> stateApplier.executeCommand(creationalCommand, executionContext));
                final List<VersionizedEvent<K>> newEvents = stateApplier.getNewEvents();
                for (final VersionizedEvent<K> newEvent : newEvents) {
                    for (final OnStoredEventListener<K, Event<K>> onStoredEventListener : onStoredEventListeners) {
                        onStoredEventListener.execute(id, newEvent.event());
                    }
                }
                eventRepository.save(newEvents, aggregate, executionContext.executedBy());
                traceAppender.append(new AggregateIdTraceable(aggregate.id()), From.from(creationalCommand));
                return aggregate;
            } catch (final SequenceGenerationException | DuplicateAggregateException | BusinessException
                           | OnStoredEventListenerException | TraceAppenderException exception) {
                throw new CommandException(exception);
            }
        });
    }

    public final A handle(final Command<K> command) throws CommandException {
        return execute(command, executionContextProvider.provide(), null);
    }

    public final A handle(final Command<K> command, final Supplier<MissingAggregateException> missingAggregateExceptionSupplier) throws CommandException {
        return execute(command, executionContextProvider.provide(), missingAggregateExceptionSupplier);
    }

    private A execute(final Command<K> command, final ExecutionContext executionContext,
                      final Supplier<MissingAggregateException> missingAggregateExceptionSupplier) throws CommandException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(executionContext);
        Validate.validState(!executionContext.executedBy().value().equals(ExecutedBy.Banned.DISCRIMINANT));
        return transaction.joiningExisting(() -> {
            try {
                final List<ExecutedByEvent<K>> events = eventRepository.loadOrderByVersionASC(command.id());
                if (events.isEmpty() && missingAggregateExceptionSupplier != null) {
                    throw missingAggregateExceptionSupplier.get();
                }
                final StateApplier<A, K> stateApplier = stateApplier(events, command.id());
                final A aggregate = commandHandlerRegistry.execute(command.id(),
                        () -> stateApplier.executeCommand(command, executionContext));
                final List<VersionizedEvent<K>> newEvents = stateApplier.getNewEvents();
                for (final VersionizedEvent<K> newEvent : newEvents) {
                    for (final OnStoredEventListener<K, Event<K>> onStoredEventListener : onStoredEventListeners) {
                        onStoredEventListener.execute(command.id(), newEvent.event());
                    }
                }
                eventRepository.save(newEvents, aggregate, executionContext.executedBy());
                traceAppender.append(new AggregateIdTraceable(aggregate.id()), From.from(command));
                return aggregate;
            } catch (final MissingAggregateException | BusinessException | OnStoredEventListenerException
                           | TraceAppenderException exception) {
                throw new CommandException(exception);
            }
        });
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
