package com.damdamdeo.pulse.extension.core.traceability;

import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Objects;

public record TraceRecorder(TraceId traceId, CorrelationId correlationId, ExecutedAt executedAt, Source source,
                            ExecutionStatus executionStatus, From from,
                            List<EncodedTraceIdentifiable> encodedTraceIdentifiables) {

    public TraceRecorder {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(executedAt);
        Objects.requireNonNull(source);
        Objects.requireNonNull(executionStatus);
        Objects.requireNonNull(from);
        Objects.requireNonNull(encodedTraceIdentifiables);
        Validate.isTrue(!encodedTraceIdentifiables.isEmpty());
        Validate.validState(encodedTraceIdentifiables.stream().map(EncodedTraceIdentifiable::source)
                .allMatch(source1 -> source1.equals(source)));
    }
}
