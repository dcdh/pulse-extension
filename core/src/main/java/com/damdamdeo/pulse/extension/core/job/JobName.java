package com.damdamdeo.pulse.extension.core.job;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record JobName(String name) {

    private static final String MATCH_PATTERN = "^[a-zA-Z]+$";

    public JobName {
        Objects.requireNonNull(name);
        Validate.matchesPattern(name, MATCH_PATTERN);
    }
}
