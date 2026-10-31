package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.usecase.permission.Everyone;
import com.damdamdeo.pulse.extension.core.usecase.permission.Permission;

import java.util.List;
import java.util.function.Function;

public final class CreateTodoDomainUseCase extends AbstractCreationalDomainUseCase<TodoId, CreateTodo, Todo> {

    CreateTodoDomainUseCase(final CommandHandler<Todo, TodoId> commandHandler) {
        super(commandHandler);
    }

    @Override
    protected Function<SequenceNumber, TodoId> creational(final CreateTodo command) {
        return sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber);
    }

    @Override
    protected Function<TodoId, DuplicateAggregateException> duplicateAggregateException() {
        return todoId -> new DuplicateTodoException();
    }

    @Override
    public List<Permission<TodoId, CreateTodo>> permissions() {
        return List.of(new Everyone<>());
    }
}
