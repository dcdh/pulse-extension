package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.EventRepository;

public class TodoCommandHandler extends CommandHandler<Todo, TodoId> {

    public TodoCommandHandler(final CommandHandlerRegistry commandHandlerRegistry,
                              final EventRepository<Todo, TodoId> eventRepository,
                              final Transaction transaction) {
        super(commandHandlerRegistry, eventRepository, transaction);
    }

    @Override
    protected Class<Todo> getAggregateClass() {
        return Todo.class;
    }
}
