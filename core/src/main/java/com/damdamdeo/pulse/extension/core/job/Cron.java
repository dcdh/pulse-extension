package com.damdamdeo.pulse.extension.core.job;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record Cron(String cron) {

    private static final String MATCH_PATTERN = "^((((\\d+,)+\\d+|(\\d+(\\/|-|#)\\d+)|\\d+L?|\\*(\\/\\d+)?|L(-\\d+)?|\\?|[A-Z]{3}(-[A-Z]{3})?) ?){5,7})$";

    public Cron {
        Objects.requireNonNull(cron);
        Validate.matchesPattern(cron, MATCH_PATTERN);
    }
}
