package com.damdamdeo.pulse.extension.core.consumer;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record LastConsumedAggregateVersion(Integer version) {

    public LastConsumedAggregateVersion {
        Objects.requireNonNull(version);
        Validate.validState(version >= 0, "Version must be greater than or equal to 0");
    }

    public boolean isBelow(final CurrentVersionInConsumption other) {
        Objects.requireNonNull(other);
        return version.compareTo(other.version()) < 0;
    }
}
