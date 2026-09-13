package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.traceability.Traceable;

import java.util.Objects;
import java.util.Set;

public record AggregateIdTraceable(AggregateId aggregateId) implements Traceable {

    public AggregateIdTraceable {
        Objects.requireNonNull(aggregateId);
    }

    @Override
    public Set<AggregateId> aggregateIds() {
        return Set.of(aggregateId);
    }
}
