package com.damdamdeo.pulse.extension.core;

import java.util.List;
import java.util.Objects;

public abstract class CommandHandler<A extends AggregateRoot<K>, K extends AggregateId> {

    private final EventRepository<A, K> eventRepository;
    private final Transaction transaction;

    public CommandHandler(final EventRepository<A, K> eventRepository,
                          final Transaction transaction) {
        this.eventRepository = Objects.requireNonNull(eventRepository);
        this.transaction = Objects.requireNonNull(transaction);
    }

    public A handle(final Command<K> command) {
        return transaction.joiningExisting(() -> {
            final List<Event<K>> events = eventRepository.loadOrderByVersionASC(command.id());
            final StateApplier<A, K> stateApplier = stateApplier(events);
            final A aggregate = stateApplier.executeCommand(command);
            List<VersionizedEvent<K>> newEvents = stateApplier.getNewEvents();
            eventRepository.save(newEvents);
            return aggregate;
        });
    }

    abstract protected Class<A> getAggregateClass();

    private StateApplier<A, K> stateApplier(List<Event<K>> events) {
        return new StateApplier<>(new ReflectionAggregateRootInstanceCreator(), events, getAggregateClass());
    }
}
