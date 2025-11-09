package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

// Composition relationship (part-of)
public record BelongsTo(AggregateId aggregateId) {

    public BelongsTo {
        Objects.requireNonNull(aggregateId);
    }
}
