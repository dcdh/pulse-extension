package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record MarkTodoAsDone(TodoId id) implements Command<TodoId> {

    public MarkTodoAsDone {
        Objects.requireNonNull(id);
    }
}
