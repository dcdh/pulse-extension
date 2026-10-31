package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.MissingAggregateException;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UnknownTodoException;
import com.damdamdeo.pulse.extension.core.command.ArchiveTodo;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.usecase.permission.Everyone;
import com.damdamdeo.pulse.extension.core.usecase.permission.Permission;

import java.util.List;
import java.util.function.Supplier;

public class ArchiveTodoDomainUseCase extends AbstractDomainUseCase<TodoId, ArchiveTodo, Todo> {

    protected ArchiveTodoDomainUseCase(final CommandHandler<Todo, TodoId> commandHandler) {
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
