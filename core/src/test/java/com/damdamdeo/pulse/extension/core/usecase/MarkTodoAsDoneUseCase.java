package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.usecase.permission.Everyone;
import com.damdamdeo.pulse.extension.core.usecase.permission.Permission;

import java.util.List;
import java.util.function.Supplier;

public class MarkTodoAsDoneUseCase extends AbstractDomainUseCase<TodoId, MarkTodoAsDone, TodoAggregateRoot> {

    protected MarkTodoAsDoneUseCase(final CommandHandler<TodoAggregateRoot, TodoId> commandHandler) {
        super(commandHandler);
    }

    @Override
    protected Supplier<MissingAggregateException> missingAggregateException() {
        return UnknownTodoException::new;
    }

    @Override
    public List<Permission<TodoId, MarkTodoAsDone>> permissions() {
        return List.of(new Everyone<>());
    }
}
