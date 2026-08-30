package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;

import java.util.Objects;

public record SingleTrace(TraceId traceId, TracedByHashed tracedByHashed, ExecutedByEncoded executedByEncoded,
                          OwnedBy ownedBy, ExecutedAt executedAt, ExecutionStatus executionStatus) {

    public SingleTrace {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(tracedByHashed);
        Objects.requireNonNull(executedByEncoded);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(executedAt);
        Objects.requireNonNull(executionStatus);
    }
}
