package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.ReflectionAggregateRootInstanceCreator;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.event.StateApplier;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;

import java.util.List;
import java.util.Objects;

public abstract class CommandHandler<A extends AggregateRoot<K>, K extends AggregateId> {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final EventRepository<A, K> eventRepository;
    private final Transaction transaction;

    public CommandHandler(final CommandHandlerRegistry commandHandlerRegistry,
                          final EventRepository<A, K> eventRepository,
                          final Transaction transaction) {
        this.commandHandlerRegistry = Objects.requireNonNull(commandHandlerRegistry);
        this.eventRepository = Objects.requireNonNull(eventRepository);
        this.transaction = Objects.requireNonNull(transaction);
    }

    public A handle(final Command<K> command) {
        return commandHandlerRegistry.execute(command.id(), () -> transaction.joiningExisting(() -> {
            final List<Event<K>> events = eventRepository.loadOrderByVersionASC(command.id());
            final StateApplier<A, K> stateApplier = stateApplier(events);
            final A aggregate = stateApplier.executeCommand(command);
            List<VersionizedEvent<K>> newEvents = stateApplier.getNewEvents();
            eventRepository.save(newEvents, aggregate);
            return aggregate;
        }));
    }

    abstract protected Class<A> getAggregateClass();

    private StateApplier<A, K> stateApplier(List<Event<K>> events) {
        return new StateApplier<>(new ReflectionAggregateRootInstanceCreator(), events, getAggregateClass());
    }
}
