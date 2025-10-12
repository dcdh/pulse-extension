package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record TodoMarkedAsDone(TodoId id) implements Event<TodoId> {

    public TodoMarkedAsDone {
        Objects.requireNonNull(id);
    }
}
