package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record EncodedTraceIdentifiable(Source source, Identifiable identifiable, ProjectionId<?> projectionId,
                                       ExecutedByHashed executedByHashed,
                                       ExecutedByEncoded executedByEncoded,
                                       EventType eventType, AggregateVersion aggregateVersion) {

    public EncodedTraceIdentifiable {
        Objects.requireNonNull(source);
        Objects.requireNonNull(identifiable);
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(executedByEncoded);
        switch (source) {
            case QUERY -> {
                Validate.isTrue(eventType == null, "eventType must be null for QUERY");
                Validate.isTrue(aggregateVersion == null, "aggregateVersion must be null for QUERY");
                Objects.requireNonNull(projectionId);
                Validate.validState(projectionId.linkTo().equals(identifiable));
            }
            case COMMAND, TRACEABILITY -> {
                Validate.isTrue(projectionId == null);
            }
        }
    }

    public boolean hasEvent() {
        return eventType != null && aggregateVersion != null;
    }

    public static EncodedTraceIdentifiable from(final Projection<?> projection, final ExecutedByHashed executedByHashed,
                                                final ExecutedByEncoded executedByEncoded) {
        return new EncodedTraceIdentifiable(Source.QUERY, projection.id().linkTo(), projection.id(), executedByHashed,
                executedByEncoded, null, null);
    }

    public static EncodedTraceIdentifiable from(final Command<?> command, final ExecutedByHashed executedByHashed,
                                                final ExecutedByEncoded executedByEncoded, final EventType eventType, final AggregateVersion aggregateVersion) {
        return new EncodedTraceIdentifiable(Source.COMMAND, command.id(), null, executedByHashed, executedByEncoded, eventType, aggregateVersion);
    }

    // Command without event
    public static EncodedTraceIdentifiable from(final Command<?> command, final ExecutedByHashed executedByHashed,
                                                final ExecutedByEncoded executedByEncoded) {
        return new EncodedTraceIdentifiable(Source.COMMAND, command.id(), null, executedByHashed, executedByEncoded, null, null);
    }

    public static EncodedTraceIdentifiable fromTraceability(final Identifiable identifiable, final ExecutedByHashed executedByHashed,
                                                            final ExecutedByEncoded executedByEncoded) {
        return new EncodedTraceIdentifiable(Source.TRACEABILITY, identifiable, null, executedByHashed, executedByEncoded, null, null);
    }
}
