package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record ClassifiedAsImportant(TodoId id) implements Event<TodoId> {

    public ClassifiedAsImportant {
        Objects.requireNonNull(id);
    }
}
