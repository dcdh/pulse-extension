package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;
import java.util.Optional;

public record AddNewTodoItem(TodoId todoId, String description) implements CreationalCommand<TodoChecklistId> {

    public AddNewTodoItem {
        Objects.requireNonNull(todoId);
        Objects.requireNonNull(description);
    }

    @Override
    public Optional<BelongsTo> belongsTo() {
        return Optional.of(BelongsTo.from(todoId));
    }
}
