package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.Objects;

public record SingleTraceDecoded(TraceId traceId, TracedByHashed tracedByHashed, ExecutedBy executedBy,
                                 ExecutedAt executedAt, ExecutionStatus executionStatus) {

    public SingleTraceDecoded {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(tracedByHashed);
        Objects.requireNonNull(executedBy);
        Objects.requireNonNull(executedAt);
        Objects.requireNonNull(executionStatus);
    }
}
