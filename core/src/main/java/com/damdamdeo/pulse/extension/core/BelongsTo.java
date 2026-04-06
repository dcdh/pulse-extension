package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

// Composition relationship (part-of)
public record BelongsTo(AggregateId aggregateId) {

    public BelongsTo {
        Objects.requireNonNull(aggregateId);
    }

    public static BelongsTo himself(final AggregateRoot<?> aggregateRoot) {
        return new BelongsTo(aggregateRoot.id());
    }
}
