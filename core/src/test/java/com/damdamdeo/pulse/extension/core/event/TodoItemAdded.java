package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.TodoChecklistId;

import java.util.Objects;

public record TodoItemAdded(String description) implements Event<TodoChecklistId> {

    public TodoItemAdded {
        Objects.requireNonNull(description);
    }
}
