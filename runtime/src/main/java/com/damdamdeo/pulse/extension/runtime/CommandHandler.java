package com.damdamdeo.pulse.extension.runtime;

import java.util.List;
import java.util.Objects;

public abstract class CommandHandler<A extends AggregateRoot<K>, K extends AggregateId<?>> {

    private final EventRepository<A, K> eventRepository;
    private final Transaction transaction;

    public CommandHandler(final EventRepository<A, K> eventRepository,
                          final Transaction transaction) {
        this.eventRepository = Objects.requireNonNull(eventRepository);
        this.transaction = Objects.requireNonNull(transaction);
    }

    public A handle(final Command<K> command) {
        return transaction.joiningExisting(() -> {
            final List<Event<K>> events = eventRepository.loadOrderByVersionASC(getAggregateClass(), command.id());
            final StateApplier<A, K> stateApplier = stateApplier(events);
            final A aggregate = stateApplier.executeCommand(command);
            List<VersionizedEvent<K>> newEvents = stateApplier.getNewEvents();
            eventRepository.save(getAggregateClass(), newEvents);
            return aggregate;
        });
    }

    abstract protected Class<A> getAggregateClass();

    abstract protected StateApplier<A, K> stateApplier(List<Event<K>> events);
}
