package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Objects;

public record EncodedInvolved(AggregateId aggregateId, EncodedActor encodedActor, NbOfTimes nbOfTimes) {

    public EncodedInvolved {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(encodedActor);
        Objects.requireNonNull(nbOfTimes);
    }
}
