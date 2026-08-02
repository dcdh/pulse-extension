package com.damdamdeo.pulse.extension.core;

import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.regex.Pattern;

// Composition relationship (part-of)
public record BelongsTo(String id) {

    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]+$");

    public BelongsTo {
        Objects.requireNonNull(id);
        Validate.validState(PATTERN.matcher(id).matches(), "invalid id '%s'".formatted(id));
    }

    public static BelongsTo from(final AggregateId belongsTo) {
        Objects.requireNonNull(belongsTo);
        return new BelongsTo(belongsTo.id());
    }

    public static BelongsTo himself(final AggregateRoot<?> aggregateRoot) {
        return BelongsTo.from(aggregateRoot.id());
    }
}
