package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record ArchiveTodo(TodoId todoId) implements Command<TodoId> {

    public ArchiveTodo {
        Objects.requireNonNull(todoId);
    }

    @Override
    public CommandId<TodoId> id() {
        return CommandId.from(todoId);
    }
}
