package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

// Composition relationship (part-of)
public record BelongsTo(String id) {

    public BelongsTo {
        Objects.requireNonNull(id);
    }

    public static BelongsTo from(final AggregateId belongsTo) {
        Objects.requireNonNull(belongsTo);
        return new BelongsTo(belongsTo.id());
    }

    public static BelongsTo himself(final AggregateRoot<?> aggregateRoot) {
        return BelongsTo.from(aggregateRoot.id());
    }
}
