package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Objects;

public record DetailedInvolved(TraceId traceId, CorrelationId correlationId, Identifiable identifiable, Actor actor,
                               EventType eventType, AggregateVersion aggregateVersion,
                               Source source, ExecutionStatus executionStatus, From from, ExecutedAt executedAt) {

    public DetailedInvolved {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(identifiable);
        Objects.requireNonNull(actor);
        // eventType can be null
        // aggregateVersion can be null
        Objects.requireNonNull(source);
        Objects.requireNonNull(executionStatus);
        Objects.requireNonNull(from);
        Objects.requireNonNull(executedAt);
    }
}
