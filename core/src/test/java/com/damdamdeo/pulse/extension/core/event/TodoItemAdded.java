package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.TodoChecklistId;

import java.util.Objects;

public record TodoItemAdded(TodoChecklistId id, String description) implements Event<TodoChecklistId> {

    public TodoItemAdded {
        Objects.requireNonNull(id);
        Objects.requireNonNull(description);
    }

    @Override
    public OwnedBy ownedBy() {
        return new OwnedBy(id.todoId().user());
    }
}
