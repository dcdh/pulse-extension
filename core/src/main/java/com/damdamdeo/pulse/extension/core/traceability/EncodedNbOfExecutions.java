package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.traceability.nboftimes.*;

import java.util.Objects;

public record EncodedNbOfExecutions(EncodedActor encodedActor,
                                    CommandNbOfTimes commandNbOfTimes,
                                    CommandUnauthorizedNbOfTimes commandUnauthorizedNbOfTimes,
                                    CommandBusinessFailedNbOfTimes commandBusinessFailedNbOfTimes,
                                    QueryNbOfTimes queryNbOfTimes,
                                    QueryUnauthorizedNbOfTimes queryUnauthorizedNbOfTimes,
                                    TraceabilityNbOfTimes traceabilityNbOfTimes) {

    public EncodedNbOfExecutions {
        Objects.requireNonNull(encodedActor);
        Objects.requireNonNull(commandNbOfTimes);
        Objects.requireNonNull(commandUnauthorizedNbOfTimes);
        Objects.requireNonNull(commandBusinessFailedNbOfTimes);
        Objects.requireNonNull(queryNbOfTimes);
        Objects.requireNonNull(queryUnauthorizedNbOfTimes);
        Objects.requireNonNull(traceabilityNbOfTimes);
    }
}

