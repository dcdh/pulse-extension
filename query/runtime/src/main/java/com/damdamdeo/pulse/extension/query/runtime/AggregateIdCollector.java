package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class AggregateIdCollector {

    private final Set<AggregateId> aggregateIds = new LinkedHashSet<>();

    public void add(final AggregateId aggregateId) {
        Objects.requireNonNull(aggregateId);
        aggregateIds.add(aggregateId);
    }

    public Set<AggregateId> aggregateId() {
        return Set.copyOf(aggregateIds);
    }
}
