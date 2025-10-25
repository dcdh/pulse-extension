package com.damdamdeo.pulse.extension.core.consumer;

import java.util.Objects;

public record LastConsumedAggregateVersion(Integer version) {

    public LastConsumedAggregateVersion {
        Objects.requireNonNull(version);
        if (version < 0) {
            throw new IllegalArgumentException("Version should be greater than or equal to zero");
        }
    }

    public boolean isBelow(final CurrentVersionInConsumption other) {
        Objects.requireNonNull(other);
        return version.compareTo(other.version()) <= 0;
    }
}
