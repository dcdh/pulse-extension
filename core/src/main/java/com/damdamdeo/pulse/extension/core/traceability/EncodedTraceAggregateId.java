package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record EncodedTraceAggregateId(Source source, AggregateId aggregateId, ProjectionId<?> projectionId,
                                      ExecutedByHashed executedByHashed,
                                      ExecutedByEncoded executedByEncoded,
                                      EventType eventType, AggregateVersion aggregateVersion) {

    public EncodedTraceAggregateId {
        Objects.requireNonNull(source);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(executedByEncoded);
        switch (source) {
            case QUERY -> {
                Validate.isTrue(eventType == null, "eventType must be null for QUERY");
                Validate.isTrue(aggregateVersion == null, "aggregateVersion must be null for QUERY");
                Objects.requireNonNull(projectionId);
                Validate.validState(projectionId.linkTo().equals(aggregateId));
            }
            case COMMAND, TRACEABILITY -> {
                Validate.isTrue(projectionId == null);
            }
        }
    }

    public boolean hasEvent() {
        return eventType != null && aggregateVersion != null;
    }

    public static EncodedTraceAggregateId from(final Projection<?> projection, final ExecutedByHashed executedByHashed,
                                               final ExecutedByEncoded executedByEncoded) {
        return new EncodedTraceAggregateId(Source.QUERY, projection.id().linkTo(), projection.id(), executedByHashed,
                executedByEncoded, null, null);
    }

    public static EncodedTraceAggregateId from(final Command<?> command, final ExecutedByHashed executedByHashed,
                                               final ExecutedByEncoded executedByEncoded, final EventType eventType, final AggregateVersion aggregateVersion) {
        return new EncodedTraceAggregateId(Source.COMMAND, command.id(), null, executedByHashed, executedByEncoded, eventType, aggregateVersion);
    }

    // Command without event
    public static EncodedTraceAggregateId from(final Command<?> command, final ExecutedByHashed executedByHashed,
                                               final ExecutedByEncoded executedByEncoded) {
        return new EncodedTraceAggregateId(Source.COMMAND, command.id(), null, executedByHashed, executedByEncoded, null, null);
    }

    public static EncodedTraceAggregateId fromTraceability(final AggregateId aggregateId, final ExecutedByHashed executedByHashed,
                                                           final ExecutedByEncoded executedByEncoded) {
        return new EncodedTraceAggregateId(Source.TRACEABILITY, aggregateId, null, executedByHashed, executedByEncoded, null, null);
    }
}
