package com.damdamdeo.pulse.extension.core.traceability;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record From(String from) {

    private static final String MATCH_PATTERN = "^[a-zA-Z]+$";

    public From {
        Objects.requireNonNull(from);
        Validate.matchesPattern(from, MATCH_PATTERN, "from must be a valid class name - current value: " + from);
    }

    public static From from(final Object from) {
        Objects.requireNonNull(from);
        return new From(from.getClass().getSimpleName());
    }
}
