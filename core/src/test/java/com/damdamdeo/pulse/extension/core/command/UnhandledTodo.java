package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record UnhandledTodo(TodoId aggregateId) implements Command<TodoId> {

    public UnhandledTodo {
        Objects.requireNonNull(aggregateId);
    }

    @Override
    public CommandId<TodoId> id() {
        return CommandId.from(aggregateId);
    }
}
