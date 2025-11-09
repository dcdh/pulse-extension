package com.damdamdeo.pulse.extension.core.consumer;

import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.regex.Pattern;

public record Target(String name) {

    public static final Pattern TARGET_PATTERN = Pattern.compile("^[a-zA-Z]+$");

    public Target {
        Objects.requireNonNull(name);
        Validate.validState(TARGET_PATTERN.matcher(name).matches());
    }
}
