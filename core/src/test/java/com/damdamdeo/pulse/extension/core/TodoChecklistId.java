package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record TodoChecklistId(TodoId todoId, Long index) implements AggregateId {

    public TodoChecklistId {
        Objects.requireNonNull(todoId);
        Objects.requireNonNull(index);
    }

    @Override
    public String id() {
        return todoId.id() + "/" + index;
    }
}
