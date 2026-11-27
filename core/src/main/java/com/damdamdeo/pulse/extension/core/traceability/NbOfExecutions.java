package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.traceability.nboftimes.*;

import java.util.Objects;

public record NbOfExecutions(ExecutedBy executedBy,
                             CommandNbOfTimes commandNbOfTimes,
                             CommandUnauthorizedNbOfTimes commandUnauthorizedNbOfTimes,
                             CommandBusinessFailedNbOfTimes commandBusinessFailedNbOfTimes,
                             QueryNbOfTimes queryNbOfTimes,
                             QueryUnauthorizedNbOfTimes queryUnauthorizedNbOfTimes,
                             TraceabilityNbOfTimes traceabilityNbOfTimes) {

    public NbOfExecutions {
        Objects.requireNonNull(executedBy);
        Objects.requireNonNull(commandNbOfTimes);
        Objects.requireNonNull(commandUnauthorizedNbOfTimes);
        Objects.requireNonNull(commandBusinessFailedNbOfTimes);
        Objects.requireNonNull(queryNbOfTimes);
        Objects.requireNonNull(queryUnauthorizedNbOfTimes);
        Objects.requireNonNull(traceabilityNbOfTimes);
    }
}
