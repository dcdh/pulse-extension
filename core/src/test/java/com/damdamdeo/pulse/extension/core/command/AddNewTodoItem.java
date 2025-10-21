package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.TodoChecklistId;

import java.util.Objects;

public record AddNewTodoItem(TodoChecklistId id, String description) implements Command<TodoChecklistId> {

    public AddNewTodoItem {
        Objects.requireNonNull(id);
        Objects.requireNonNull(description);
    }
}
