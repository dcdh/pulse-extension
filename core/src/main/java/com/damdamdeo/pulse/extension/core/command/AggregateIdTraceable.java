package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.traceability.Traceable;

import java.util.Objects;
import java.util.Set;

public record AggregateIdTraceable<A extends AggregateId>(A aggregateId) implements Traceable<A> {

    public AggregateIdTraceable {
        Objects.requireNonNull(aggregateId);
    }

    @Override
    public Set<A> aggregateIds() {
        return Set.of(aggregateId);
    }
}
