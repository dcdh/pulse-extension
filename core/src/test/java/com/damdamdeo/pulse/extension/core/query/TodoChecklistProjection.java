package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.TodoChecklistId;

import java.util.Objects;

public record TodoChecklistProjection(ProjectionId<TodoChecklistId> todoChecklistId,
                                      String description) implements Projection<TodoChecklistId> {

    public TodoChecklistProjection {
        Objects.requireNonNull(todoChecklistId);
        Objects.requireNonNull(description);
    }

    @Override
    public ProjectionId<TodoChecklistId> id() {
        return todoChecklistId;
    }
}
