package com.damdamdeo.pulse.extension.runtime;

import java.util.Objects;

public record TodoMarkedAsDone(TodoId id) implements Event<TodoId> {

    public TodoMarkedAsDone {
        Objects.requireNonNull(id);
    }
}
