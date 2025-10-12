package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record MarkTodoAsDone(TodoId id) implements Command<TodoId> {

    public MarkTodoAsDone {
        Objects.requireNonNull(id);
    }
}
