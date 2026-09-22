package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Objects;

public record Involved(AggregateId aggregateId, Actor actor, NbOfTimes commandNbOfTimes, NbOfTimes queryNbOfTimes) {

    public Involved {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(actor);
        Objects.requireNonNull(commandNbOfTimes);
        Objects.requireNonNull(queryNbOfTimes);
    }
}
