package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.ArchiveTodo;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.usecase.permission.Everyone;
import com.damdamdeo.pulse.extension.core.usecase.permission.Permission;

import java.util.List;
import java.util.function.Supplier;

public class ArchiveTodoDomainUseCase extends AbstractDomainUseCase<TodoId, ArchiveTodo, TodoAggregateRoot> {

    protected ArchiveTodoDomainUseCase(final CommandHandler<TodoAggregateRoot, TodoId> commandHandler) {
        super(commandHandler);
    }

    @Override
    protected Supplier<MissingAggregateException> missingAggregateException() {
        return UnknownTodoException::new;
    }

    @Override
    public List<Permission<TodoId, ArchiveTodo>> permissions() {
        return List.of(new Everyone<>());
    }
}
