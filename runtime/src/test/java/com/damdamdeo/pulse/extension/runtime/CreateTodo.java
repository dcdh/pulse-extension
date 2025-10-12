package com.damdamdeo.pulse.extension.runtime;

import java.util.Objects;

public record CreateTodo(TodoId id, String description) implements Command<TodoId> {

    public CreateTodo {
        Objects.requireNonNull(id);
        Objects.requireNonNull(description);
    }
}
