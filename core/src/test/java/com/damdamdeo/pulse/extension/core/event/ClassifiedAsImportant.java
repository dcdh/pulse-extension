package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record ClassifiedAsImportant(TodoId id) implements Event<TodoId> {

    public ClassifiedAsImportant {
        Objects.requireNonNull(id);
    }

    @Override
    public OwnedBy ownedBy() {
        return new OwnedBy(id.user());
    }
}
