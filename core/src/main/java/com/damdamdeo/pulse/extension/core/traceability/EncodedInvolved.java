package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.traceability.nboftimes.*;

import java.util.Objects;

public record EncodedInvolved(AggregateId aggregateId, EncodedActor encodedActor,
                              CommandNbOfTimes commandNbOfTimes,
                              CommandUnauthorizedNbOfTimes commandUnauthorizedNbOfTimes,
                              CommandBusinessFailedNbOfTimes commandBusinessFailedNbOfTimes,
                              QueryNbOfTimes queryNbOfTimes,
                              QueryUnauthorizedNbOfTimes queryUnauthorizedNbOfTimes) {

    public EncodedInvolved {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(encodedActor);
        Objects.requireNonNull(commandNbOfTimes);
        Objects.requireNonNull(commandUnauthorizedNbOfTimes);
        Objects.requireNonNull(commandBusinessFailedNbOfTimes);
        Objects.requireNonNull(queryNbOfTimes);
        Objects.requireNonNull(queryUnauthorizedNbOfTimes);
    }
}
