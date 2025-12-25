package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoChecklist;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByProvider;

public final class TodoChecklistCommandHandler extends CommandHandler<TodoChecklist, TodoChecklistId> {

    public TodoChecklistCommandHandler(final CommandHandlerRegistry commandHandlerRegistry,
                                       final EventRepository<TodoChecklist, TodoChecklistId> eventRepository,
                                       final Transaction transaction,
                                       final ExecutedByProvider executedByProvider) {
        super(commandHandlerRegistry, eventRepository, transaction, executedByProvider);
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

