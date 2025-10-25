package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;

import java.util.Objects;

public final class UnknownAggregateRootException extends RuntimeException {
    private final AggregateRootType aggregateRootType;
    private final AggregateId aggregateId;

    public UnknownAggregateRootException(final AggregateRootType aggregateRootType, final AggregateId aggregateId) {
        this.aggregateRootType = Objects.requireNonNull(aggregateRootType);
        this.aggregateId = Objects.requireNonNull(aggregateId);
    }

    public AggregateRootType aggregateRootType() {
        return aggregateRootType;
    }

    public AggregateId aggregateId() {
        return aggregateId;
    }
}
