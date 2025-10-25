package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Objects;

public record AnyAggregateId(String id) implements AggregateId {

    public AnyAggregateId {
        Objects.requireNonNull(id);
    }
}
