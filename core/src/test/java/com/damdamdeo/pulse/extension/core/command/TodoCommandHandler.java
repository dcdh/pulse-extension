package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateIdGenerator;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.saga.Saga;

import java.util.List;

public class TodoCommandHandler extends CommandHandler<Todo, TodoId> {

    public TodoCommandHandler(final CommandHandlerRegistry commandHandlerRegistry,
                              final EventRepository<Todo, TodoId> eventRepository,
                              final Transaction transaction,
                              final ExecutionContextProvider executionContextProvider,
                              final List<Saga<TodoId, Event<TodoId>>> sagas,
                              final AggregateIdGenerator aggregateIdGenerator) {
        super(commandHandlerRegistry, eventRepository, transaction, executionContextProvider, sagas, aggregateIdGenerator);
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
