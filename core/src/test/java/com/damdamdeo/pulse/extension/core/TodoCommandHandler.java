package com.damdamdeo.pulse.extension.core;

import java.util.List;

public class TodoCommandHandler extends CommandHandler<Todo, TodoId> {

    public TodoCommandHandler(final EventRepository<Todo, TodoId> eventRepository,
                              final Transaction transaction) {
        super(eventRepository, transaction);
    }

    @Override
    protected Class<Todo> getAggregateClass() {
        return Todo.class;
    }

    @Override
    protected StateApplier<Todo, TodoId> stateApplier(List<Event<TodoId>> events) {
        return new TodoStateApplier(events);
    }
}
