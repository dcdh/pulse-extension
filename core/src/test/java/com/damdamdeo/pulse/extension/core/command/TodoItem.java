package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoChecklistId;

import java.util.Objects;

public record TodoItem(TodoChecklistId id, String description) {

    public TodoItem {
        Objects.requireNonNull(id);
        Objects.requireNonNull(description);
    }
}
