package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record NewTodoCreated(TodoId id, String description) implements Event<TodoId> {

    public NewTodoCreated {
        Objects.requireNonNull(id);
        Objects.requireNonNull(description);
    }
}
