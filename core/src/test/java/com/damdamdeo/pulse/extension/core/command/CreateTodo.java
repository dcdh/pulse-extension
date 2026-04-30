package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record CreateTodo(String description) implements CreationalCommand<TodoId> {

    public CreateTodo {
        Objects.requireNonNull(description);
    }
}
