package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Objects;

public record AnyAggregateId(String simpleName, String id) {

    public AnyAggregateId {
        Objects.requireNonNull(simpleName);
        Objects.requireNonNull(id);
    }

    public static AnyAggregateId from(final AggregateId aggregateId) {
        Objects.requireNonNull(aggregateId);
        return new AnyAggregateId(aggregateId.getClass().getSimpleName(), aggregateId.id());
    }
}
