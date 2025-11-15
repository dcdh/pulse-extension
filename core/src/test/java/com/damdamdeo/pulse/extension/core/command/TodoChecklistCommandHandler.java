package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoChecklist;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.event.EventRepository;

public final class TodoChecklistCommandHandler extends CommandHandler<TodoChecklist, TodoChecklistId> {

    public TodoChecklistCommandHandler(final CommandHandlerRegistry commandHandlerRegistry,
                                       final EventRepository<TodoChecklist, TodoChecklistId> eventRepository,
                                       final Transaction transaction) {
        super(commandHandlerRegistry, eventRepository, transaction);
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

