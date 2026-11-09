package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record MarkTodoAsDone(TodoId aggregateId) implements Command<TodoId> {

    public MarkTodoAsDone {
        Objects.requireNonNull(aggregateId);
    }

    @Override
    public CommandId<TodoId> id() {
        return CommandId.from(aggregateId);
    }
}
