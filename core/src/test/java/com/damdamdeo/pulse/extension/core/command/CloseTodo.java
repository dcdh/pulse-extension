package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record CloseTodo(TodoId id) implements Command<TodoId> {

    public CloseTodo {
        Objects.requireNonNull(id);
    }
}
