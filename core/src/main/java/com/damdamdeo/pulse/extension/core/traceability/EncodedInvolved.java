package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;

import java.util.Objects;

public record EncodedInvolved(AggregateId aggregateId, ExecutedByHashed executedByHashed,
                              ExecutedByEncoded executedByEncoded) {

    public EncodedInvolved {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(executedByEncoded);
    }
}
