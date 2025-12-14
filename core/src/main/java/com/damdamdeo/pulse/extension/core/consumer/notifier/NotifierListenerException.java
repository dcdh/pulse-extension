package com.damdamdeo.pulse.extension.core.consumer.notifier;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.event.EventType;

import java.util.Objects;

public class NotifierListenerException extends RuntimeException {

    private final FromApplication fromApplication;
    private final AggregateId aggregateId;
    private final AggregateRootType aggregateRootType;
    private final CurrentVersionInConsumption currentVersionInConsumption;
    private final EventType eventType;

    public NotifierListenerException(final FromApplication fromApplication,
                                     final AggregateId aggregateId,
                                     final AggregateRootType aggregateRootType,
                                     final CurrentVersionInConsumption currentVersionInConsumption,
                                     final EventType eventType,
                                     final Throwable cause) {
        super(cause);
        this.fromApplication = Objects.requireNonNull(fromApplication);
        this.aggregateId = Objects.requireNonNull(aggregateId);
        this.aggregateRootType = Objects.requireNonNull(aggregateRootType);
        this.currentVersionInConsumption = Objects.requireNonNull(currentVersionInConsumption);
        this.eventType = Objects.requireNonNull(eventType);
    }

    public FromApplication fromApplication() {
        return fromApplication;
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
