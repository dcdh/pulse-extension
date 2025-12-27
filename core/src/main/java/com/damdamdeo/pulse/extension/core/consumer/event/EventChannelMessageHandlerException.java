package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import com.damdamdeo.pulse.extension.core.event.EventType;

import java.util.Objects;

public final class EventChannelMessageHandlerException extends RuntimeException {

    private final Target target;
    private final AggregateId aggregateId;
    private final AggregateRootType aggregateRootType;
    private final CurrentVersionInConsumption currentVersionInConsumption;
    private final EventType eventType;

    public EventChannelMessageHandlerException(final Target target,
                                               final AggregateId aggregateId,
                                               final AggregateRootType aggregateRootType,
                                               final CurrentVersionInConsumption currentVersionInConsumption,
                                               final EventType eventType,
                                               final Throwable cause) {
        super(cause);
        this.target = Objects.requireNonNull(target);
        this.aggregateId = Objects.requireNonNull(aggregateId);
        this.aggregateRootType = Objects.requireNonNull(aggregateRootType);
        this.currentVersionInConsumption = Objects.requireNonNull(currentVersionInConsumption);
        this.eventType = Objects.requireNonNull(eventType);
    }

    public Target target() {
        return target;
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

    public EventType eventType() {
        return eventType;
    }
}
