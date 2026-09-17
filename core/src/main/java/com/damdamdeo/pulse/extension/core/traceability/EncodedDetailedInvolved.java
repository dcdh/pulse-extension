package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Objects;

public record EncodedDetailedInvolved(TraceId traceId, AggregateId aggregateId, EncodedActor encodedActor, From from,
                                      ExecutedAt executedAt) {

    public EncodedDetailedInvolved {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(encodedActor);
        Objects.requireNonNull(from);
        Objects.requireNonNull(executedAt);
    }
}
