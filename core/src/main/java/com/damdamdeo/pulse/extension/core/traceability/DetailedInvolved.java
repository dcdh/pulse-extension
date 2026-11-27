package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.event.EventType;

import java.util.Objects;

public record DetailedInvolved(TraceId traceId,
                               CorrelationId correlationId,
                               Actor actor,
                               EventType eventType,
                               AggregateVersion aggregateVersion,
                               Source source,
                               ExecutionStatus executionStatus,
                               From from,
                               ExecutedAt executedAt,
                               NbOfExecutions nbOfExecutions) {

    public DetailedInvolved {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(actor);
        // eventType can be null
        // aggregateVersion can be null
        Objects.requireNonNull(source);
        Objects.requireNonNull(executionStatus);
        Objects.requireNonNull(from);
        Objects.requireNonNull(executedAt);
        Objects.requireNonNull(nbOfExecutions);
    }
}
