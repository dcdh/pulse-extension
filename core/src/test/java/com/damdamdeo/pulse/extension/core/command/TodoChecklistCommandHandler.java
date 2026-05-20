package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateIdGenerator;
import com.damdamdeo.pulse.extension.core.TodoChecklist;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.saga.OnStoredEventListener;

import java.util.List;

public final class TodoChecklistCommandHandler extends CommandHandler<TodoChecklist, TodoChecklistId> {

    public TodoChecklistCommandHandler(final CommandHandlerRegistry commandHandlerRegistry,
                                       final EventRepository<TodoChecklist, TodoChecklistId> eventRepository,
                                       final Transaction transaction,
                                       final ExecutionContextProvider executionContextProvider,
                                       final List<OnStoredEventListener<TodoChecklistId, Event<TodoChecklistId>>> onStoredEventListeners,
                                       final AggregateIdGenerator aggregateIdGenerator) {
        super(commandHandlerRegistry, eventRepository, transaction, executionContextProvider, onStoredEventListeners, aggregateIdGenerator);
    }

    @Override
    protected Class<TodoChecklist> getAggregateRootClass() {
        return TodoChecklist.class;
    }

    @Override
    protected Class<TodoChecklistId> getAggregateIdClass() {
        return TodoChecklistId.class;
    }
}

