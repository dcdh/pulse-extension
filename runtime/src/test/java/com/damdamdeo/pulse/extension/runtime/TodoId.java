package com.damdamdeo.pulse.extension.runtime;

import java.util.Objects;
import java.util.UUID;

public record TodoId(UUID id) implements AggregateId<UUID> {

    public TodoId {
        Objects.requireNonNull(id);
    }
}
