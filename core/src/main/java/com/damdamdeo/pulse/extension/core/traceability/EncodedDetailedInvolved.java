package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Objects;

public record EncodedDetailedInvolved(TraceId traceId, CorrelationId correlationId,
                                      Identifiable identifiable, AnyAggregateId linkTo,
                                      EncodedActor encodedActor,
                                      EventType eventType, AggregateVersion aggregateVersion,
                                      Source source, ExecutionStatus executionStatus, From from,
                                      ExecutedAt executedAt) {

    public EncodedDetailedInvolved {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(identifiable);
        Objects.requireNonNull(linkTo);
        Objects.requireNonNull(encodedActor);
        // eventType can be null
        // aggregateVersion can be null
        Objects.requireNonNull(source);
        Objects.requireNonNull(executionStatus);
        Objects.requireNonNull(from);
        Objects.requireNonNull(executedAt);
    }
}
