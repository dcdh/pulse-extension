package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;

import java.util.Objects;

public final class AggregateRootChannelMessageHandlerException extends RuntimeException {

    private final Purpose purpose;
    private final AggregateId aggregateId;
    private final AggregateRootType aggregateRootType;
    private final CurrentVersionInConsumption currentVersionInConsumption;

    public AggregateRootChannelMessageHandlerException(final Purpose purpose,
                                                       final AggregateId aggregateId,
                                                       final AggregateRootType aggregateRootType,
                                                       final CurrentVersionInConsumption currentVersionInConsumption,
                                                       final Throwable cause) {
        super(cause);
        this.purpose = Objects.requireNonNull(purpose);
        this.aggregateId = Objects.requireNonNull(aggregateId);
        this.aggregateRootType = Objects.requireNonNull(aggregateRootType);
        this.currentVersionInConsumption = Objects.requireNonNull(currentVersionInConsumption);
    }

    public Purpose target() {
        return purpose;
    }

    public AggregateId aggregateId() {
        return aggregateId;
    }

    public AggregateRootType aggregateRootType() {
        return aggregateRootType;
    }

    public CurrentVersionInConsumption currentVersionInConsumption() {
        return currentVersionInConsumption;
    }
}
