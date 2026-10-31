package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record ArchiveTodo(TodoId id) implements Command<TodoId> {

    public ArchiveTodo {
        Objects.requireNonNull(id);
    }
}
