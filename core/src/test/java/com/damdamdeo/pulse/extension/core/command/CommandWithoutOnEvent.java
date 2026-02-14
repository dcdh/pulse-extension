package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record CommandWithoutOnEvent(TodoId id) implements Command<TodoId> {

    public CommandWithoutOnEvent {
        Objects.requireNonNull(id);
    }
}
