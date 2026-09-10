package com.damdamdeo.pulse.extension.core.traceability;

import java.util.List;
import java.util.Objects;

public record TraceRecorder(TraceId traceId, ExecutedAt executedAt, From from,
                            List<EncodedTraceAggregateId> encodedTraceAggregateIds) {

    public TraceRecorder {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(executedAt);
        Objects.requireNonNull(from);
        Objects.requireNonNull(encodedTraceAggregateIds);
    }

    public static TraceRecorder from(final TraceId traceId, final ExecutedAt executedAt, final Traceable traceable,
                                     final List<EncodedTraceAggregateId> encodedTraceAggregateIds) {
        return new TraceRecorder(traceId, executedAt, From.from(traceable), encodedTraceAggregateIds);
    }
}
