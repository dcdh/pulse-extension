package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record AggregateVersion(Integer version) {
    public AggregateVersion {
        Objects.requireNonNull(version);
        if (version < 0) {
            throw new IllegalArgumentException("Version should be greater than or equal to zero");
        }
    }

    public AggregateVersion increment() {
        return new AggregateVersion(version + 1);
    }
}
