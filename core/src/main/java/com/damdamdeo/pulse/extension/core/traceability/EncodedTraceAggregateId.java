package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;

import java.util.Objects;

public record EncodedTraceAggregateId(AnySimpleTypeAggregateId aggregateId, ExecutedByHashed executedByHashed,
                                      ExecutedByEncoded executedByEncoded) {

    public EncodedTraceAggregateId {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(executedByEncoded);
    }
}
