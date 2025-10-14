package com.damdamdeo.pulse.extension.core;

public class TodoCommandHandler extends CommandHandler<Todo, TodoId> {

    public TodoCommandHandler(final EventRepository<Todo, TodoId> eventRepository,
                              final Transaction transaction) {
        super(eventRepository, transaction);
    }

    @Override
    protected Class<Todo> getAggregateClass() {
        return Todo.class;
    }
}
