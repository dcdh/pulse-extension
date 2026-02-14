package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record FailTodo(TodoId id) implements Command<TodoId> {

    public FailTodo {
        Objects.requireNonNull(id);
    }
}
