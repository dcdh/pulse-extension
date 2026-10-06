package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record EncodedTraceAggregateId(Source source, AggregateId aggregateId, ExecutedByHashed executedByHashed,
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
            }
            case COMMAND -> {
            }
        }
    }

    public boolean hasEvent() {
        return eventType != null && aggregateVersion != null;
    }

    public static EncodedTraceAggregateId fromQuery(final AggregateId aggregateId, final ExecutedByHashed executedByHashed,
                                                    final ExecutedByEncoded executedByEncoded) {
        return new EncodedTraceAggregateId(Source.QUERY, aggregateId, executedByHashed, executedByEncoded, null, null);
    }

    public static EncodedTraceAggregateId fromCommand(final AggregateId aggregateId, final ExecutedByHashed executedByHashed,
                                                      final ExecutedByEncoded executedByEncoded, final EventType eventType, final AggregateVersion aggregateVersion) {
        return new EncodedTraceAggregateId(Source.COMMAND, aggregateId, executedByHashed, executedByEncoded, eventType, aggregateVersion);
    }

    public static EncodedTraceAggregateId fromCommand(final AggregateId aggregateId, final ExecutedByHashed executedByHashed,
                                                      final ExecutedByEncoded executedByEncoded) {
        return new EncodedTraceAggregateId(Source.COMMAND, aggregateId, executedByHashed, executedByEncoded, null, null);
    }
}
