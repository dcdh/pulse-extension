package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;

import java.util.Objects;

// , TracedByHashed tracedByHashed, ExecutedByEncoded executedByEncoded
public record AccessedAggregate(AggregateId aggregateId, OwnedBy ownedBy) {

    public AccessedAggregate {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(executedByEncoded);
        Objects.requireNonNull(ownedBy);
    }
}
