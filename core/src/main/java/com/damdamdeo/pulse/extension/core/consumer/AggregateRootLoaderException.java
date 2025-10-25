package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;

import java.util.Objects;

public class AggregateRootLoaderException extends RuntimeException {
    private final AggregateRootType aggregateRootType;
    private final AggregateId aggregateId;

    public AggregateRootLoaderException(final AggregateRootType aggregateRootType, final AggregateId aggregateId, final Throwable cause) {
        super(cause);
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
