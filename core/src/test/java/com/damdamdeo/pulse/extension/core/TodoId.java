package com.damdamdeo.pulse.extension.core;

import java.util.Objects;
import java.util.UUID;

public record TodoId(UUID id) implements AggregateId<UUID> {

    public TodoId {
        Objects.requireNonNull(id);
    }
}
