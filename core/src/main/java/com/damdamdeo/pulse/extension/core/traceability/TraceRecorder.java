package com.damdamdeo.pulse.extension.core.traceability;

import java.util.List;
import java.util.Objects;

public record TraceRecorder(TraceId traceId, ExecutedAt executedAt, Source source, From from,
                            List<EncodedTraceAggregateId> encodedTraceAggregateIds) {

    public TraceRecorder {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(executedAt);
        Objects.requireNonNull(source);
        Objects.requireNonNull(from);
        Objects.requireNonNull(encodedTraceAggregateIds);
    }
}
