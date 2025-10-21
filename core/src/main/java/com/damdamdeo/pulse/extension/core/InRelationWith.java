package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record InRelationWith(AggregateId aggregateId) {

    public InRelationWith {
        Objects.requireNonNull(aggregateId);
    }
}
