package com.damdamdeo.pulse.extension.runtime;

import java.util.Objects;

public record MarkTodoAsDone(TodoId id) implements Command<TodoId> {

    public MarkTodoAsDone {
        Objects.requireNonNull(id);
    }
}
