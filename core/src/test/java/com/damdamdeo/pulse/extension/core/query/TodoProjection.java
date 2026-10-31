package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.List;
import java.util.Objects;

public record TodoProjection(ProjectionId<TodoId> todoId,
                             String description,
                             Status status,
                             Boolean important,
                             List<TodoChecklistProjection> checklist) implements Projection<TodoId> {

    public TodoProjection {
        Objects.requireNonNull(todoId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(status);
        Objects.requireNonNull(important);
        Objects.requireNonNull(checklist);
    }

    public TodoProjection withStatus(final Status status) {
        return new TodoProjection(todoId, description, status, important, checklist);
    }

    @Override
    public ProjectionId<TodoId> id() {
        return todoId;
    }
}
