package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;

import java.util.Objects;

public record Involved(AnyAggregateId aggregateId, ExecutedByHashed executedByHashed, ExecutedBy executedBy) {

    public Involved {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(executedBy);
    }
}
