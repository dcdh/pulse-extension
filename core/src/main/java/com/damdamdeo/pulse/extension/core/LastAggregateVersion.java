package com.damdamdeo.pulse.extension.core;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record LastAggregateVersion(Integer version) {

    public LastAggregateVersion {
        Objects.requireNonNull(version);
        Validate.validState(version >= 0, "Version must be greater than or equal to 0");
    }
}
