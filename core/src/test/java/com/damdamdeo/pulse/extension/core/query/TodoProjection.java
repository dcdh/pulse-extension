package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.List;
import java.util.Objects;

public record TodoProjection(ProjectionId<TodoId> projectionId,
                             Description description,
                             Status status,
                             boolean important,
                             List<TodoChecklistProjection> checklist,
                             Owner owner) implements Projection<TodoId>, Todo {

    public TodoProjection {
        Objects.requireNonNull(projectionId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(status);
        Objects.requireNonNull(checklist);
        Objects.requireNonNull(owner);
    }

    public TodoProjection withStatus(final Status status) {
        return new TodoProjection(projectionId, description, status, important, checklist, owner);
    }

    @Override
    public TodoId todoId() {
        return projectionId.linkTo();
    }

    @Override
    public boolean isInProgress() {
        return Status.IN_PROGRESS.equals(status);
    }

    @Override
    public ProjectionId<TodoId> id() {
        return projectionId;
    }
}
