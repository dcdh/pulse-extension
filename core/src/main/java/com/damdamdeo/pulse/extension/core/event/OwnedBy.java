package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Objects;
import java.util.regex.Pattern;

public record OwnedBy(String id) {

    private static final Pattern PATTERN = Pattern.compile("^[ a-zA-Z0-9_-]+$");

    public OwnedBy {
        Objects.requireNonNull(id);
        if (!PATTERN.matcher(id).matches()) {
            throw new IllegalStateException("invalid id");
        }
    }

    public static OwnedBy from(final AggregateId ownedBy) {
        Objects.requireNonNull(ownedBy);
        return new OwnedBy(ownedBy.id());
    }
}
