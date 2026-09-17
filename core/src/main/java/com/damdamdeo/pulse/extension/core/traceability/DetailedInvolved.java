package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Objects;

public record DetailedInvolved(TraceId traceId, AggregateId aggregateId, Actor actor, From from,
                               ExecutedAt executedAt) {

    public DetailedInvolved {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(actor);
        Objects.requireNonNull(from);
        Objects.requireNonNull(executedAt);
    }
}
