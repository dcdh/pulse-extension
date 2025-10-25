package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record LastAggregateVersion(Integer version) {

    public LastAggregateVersion {
        Objects.requireNonNull(version);
        if (version < 0) {
            throw new IllegalArgumentException("Version should be greater than or equal to zero");
        }
    }
}
