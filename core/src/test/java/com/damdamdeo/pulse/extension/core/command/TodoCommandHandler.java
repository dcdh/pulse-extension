package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.EventNotifier;
import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;

public class TodoCommandHandler extends CommandHandler<Todo, TodoId> {

    public TodoCommandHandler(final CommandHandlerRegistry commandHandlerRegistry,
                              final EventRepository<Todo, TodoId> eventRepository,
                              final Transaction transaction,
                              final ExecutionContextProvider executionContextProvider,
                              final EventNotifier eventNotifier) {
        super(commandHandlerRegistry, eventRepository, transaction, executionContextProvider, eventNotifier);
    }

    @Override
    protected Class<Todo> getAggregateRootClass() {
        return Todo.class;
    }

    @Override
    protected Class<TodoId> getAggregateIdClass() {
        return TodoId.class;
    }
}
