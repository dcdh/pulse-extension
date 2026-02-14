package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record UnhandledTodo(TodoId id) implements Command<TodoId> {

    public UnhandledTodo {
        Objects.requireNonNull(id);
    }
}

