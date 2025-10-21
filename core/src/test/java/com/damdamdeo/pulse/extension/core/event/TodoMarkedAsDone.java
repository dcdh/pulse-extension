package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record TodoMarkedAsDone(TodoId id) implements Event<TodoId> {

    public TodoMarkedAsDone {
        Objects.requireNonNull(id);
    }

    @Override
    public OwnedBy ownedBy() {
        return new OwnedBy(id.user());
    }
}
