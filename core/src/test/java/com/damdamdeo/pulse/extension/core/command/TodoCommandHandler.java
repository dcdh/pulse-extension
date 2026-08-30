package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateIdGenerator;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.saga.OnStoredEventListener;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppender;

import java.util.List;

public class TodoCommandHandler extends CommandHandler<Todo, TodoId> {

    public TodoCommandHandler(final CommandHandlerRegistry commandHandlerRegistry,
                              final EventRepository<Todo, TodoId> eventRepository,
                              final Transaction transaction,
                              final ExecutionContextProvider executionContextProvider,
                              final List<OnStoredEventListener<TodoId, Event<TodoId>>> onStoredEventListeners,
                              final AggregateIdGenerator aggregateIdGenerator,
                              final TraceAppender traceAppender) {
        super(commandHandlerRegistry, eventRepository, transaction, executionContextProvider, onStoredEventListeners,
                aggregateIdGenerator, traceAppender);
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
