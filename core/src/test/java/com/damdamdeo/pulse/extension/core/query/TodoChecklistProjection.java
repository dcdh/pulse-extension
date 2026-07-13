package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.TodoChecklistId;

import java.util.Objects;

public record TodoChecklistProjection(TodoChecklistId todoChecklistId, String description) implements Projection {

    public TodoChecklistProjection {
        Objects.requireNonNull(todoChecklistId);
        Objects.requireNonNull(description);
    }
}
