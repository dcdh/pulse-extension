package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Objects;

public record DetailedInvolved(TraceId traceId, AggregateId aggregateId, Actor actor, Source source, From from,
                               ExecutedAt executedAt) {

    public DetailedInvolved {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(actor);
        Objects.requireNonNull(source);
        Objects.requireNonNull(from);
        Objects.requireNonNull(executedAt);
    }
}
