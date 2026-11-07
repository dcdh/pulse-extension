package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Objects;

public record AnyAggregateId(String id) implements AggregateId {

    public AnyAggregateId {
        Objects.requireNonNull(id);
    }

    public static AnyAggregateId from(final AggregateId aggregateId) {
        return new AnyAggregateId(aggregateId.id());
    }

    public static AnyAggregateId from(final Identifiable identifiable) {
        return new AnyAggregateId(identifiable.id());
    }

    public static AnyAggregateId from(final String id) {
        return new AnyAggregateId(id);
    }
}
