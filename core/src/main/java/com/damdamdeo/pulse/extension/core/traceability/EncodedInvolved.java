package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;

import java.util.Objects;

public record EncodedInvolved(AnyAggregateId aggregateId, ExecutedByHashed executedByHashed,
                              ExecutedByEncoded executedByEncoded) {

    public EncodedInvolved {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(executedByEncoded);
    }
}
