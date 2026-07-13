package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.List;
import java.util.Objects;

public record TodoProjection(TodoId todoId,
                             String description,
                             Status status,
                             Boolean important,
                             List<TodoChecklistProjection> checklist) implements Projection {

    public TodoProjection {
        Objects.requireNonNull(todoId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(status);
        Objects.requireNonNull(important);
        Objects.requireNonNull(checklist);
    }

    @Override
    public AggregateId id() {
        return todoId;
    }
}
