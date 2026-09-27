package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.traceability.nboftimes.*;

import java.util.Objects;

public record Involved(AggregateId aggregateId, Actor actor,
                       CommandNbOfTimes commandNbOfTimes,
                       CommandUnauthorizedNbOfTimes commandUnauthorizedNbOfTimes,
                       CommandBusinessFailedNbOfTimes commandBusinessFailedNbOfTimes,
                       QueryNbOfTimes queryNbOfTimes,
                       QueryUnauthorizedNbOfTimes queryUnauthorizedNbOfTimes) {

    public Involved {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(actor);
        Objects.requireNonNull(commandNbOfTimes);
        Objects.requireNonNull(commandUnauthorizedNbOfTimes);
        Objects.requireNonNull(commandBusinessFailedNbOfTimes);
        Objects.requireNonNull(queryNbOfTimes);
        Objects.requireNonNull(queryUnauthorizedNbOfTimes);
    }
}
