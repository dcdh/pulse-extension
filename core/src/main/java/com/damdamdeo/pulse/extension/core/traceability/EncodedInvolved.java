package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.traceability.nboftimes.*;

import java.util.Objects;

public record EncodedInvolved(Identifiable identifiable, EncodedActor encodedActor,
                              CommandNbOfTimes commandNbOfTimes,
                              CommandUnauthorizedNbOfTimes commandUnauthorizedNbOfTimes,
                              CommandBusinessFailedNbOfTimes commandBusinessFailedNbOfTimes,
                              QueryNbOfTimes queryNbOfTimes,
                              QueryUnauthorizedNbOfTimes queryUnauthorizedNbOfTimes,
                              TraceabilityNbOfTimes traceabilityNbOfTimes) {

    public EncodedInvolved {
        Objects.requireNonNull(identifiable);
        Objects.requireNonNull(encodedActor);
        Objects.requireNonNull(commandNbOfTimes);
        Objects.requireNonNull(commandUnauthorizedNbOfTimes);
        Objects.requireNonNull(commandBusinessFailedNbOfTimes);
        Objects.requireNonNull(queryNbOfTimes);
        Objects.requireNonNull(queryUnauthorizedNbOfTimes);
        Objects.requireNonNull(traceabilityNbOfTimes);
    }
}
