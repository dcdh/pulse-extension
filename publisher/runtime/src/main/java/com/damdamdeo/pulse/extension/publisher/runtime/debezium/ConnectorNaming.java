package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.regex.Pattern;

public record ConnectorNaming(String name) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z_]+$");

    public ConnectorNaming {
        Objects.requireNonNull(name);
        Validate.validState(PATTERN.matcher(name).matches());
    }
}
