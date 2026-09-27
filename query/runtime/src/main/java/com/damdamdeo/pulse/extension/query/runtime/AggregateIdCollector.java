package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class AggregateIdCollector<A extends AggregateId> {

    private final Set<A> aggregateIds = new LinkedHashSet<>();

    private final Class<A> target;

    public AggregateIdCollector(final Class<A> target) {
        this.target = Objects.requireNonNull(target);
    }

    public void add(final A aggregateId) {
        Objects.requireNonNull(aggregateId);
        if (target.isAssignableFrom(aggregateId.getClass())) {
            aggregateIds.add(aggregateId);
        }
    }

    public Set<A> aggregateId() {
        return Set.copyOf(aggregateIds);
    }
}
