package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record TodoItemAdded(String description) implements Event<TodoId> {

    public TodoItemAdded {
        Objects.requireNonNull(description);
    }
}
