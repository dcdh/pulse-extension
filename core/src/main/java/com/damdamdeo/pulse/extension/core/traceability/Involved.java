package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Objects;

public record Involved(AggregateId aggregateId, Actor actor, NbOfTimes nbOfTimes) {

    public Involved {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(actor);
        Objects.requireNonNull(nbOfTimes);
    }
}
