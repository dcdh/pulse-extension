package com.damdamdeo.pulse.extension.core;

import java.util.Objects;
import java.util.UUID;

public record TodoId(String id) implements AggregateId {

    public TodoId {
        Objects.requireNonNull(id);
    }

    public static TodoId from(final UUID id) {
        return new TodoId(id.toString());
    }
}
