package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record Result<T extends Projection>(List<T> projections) {

    public Result {
        Objects.requireNonNull(projections);
    }

    public T getFirst() {
        return projections.getFirst();
    }

    public int count() {
        return projections.size();
    }

    public Set<AggregateId> aggregateIds() {
        return projections.stream().map(Projection::aggregateIds).flatMap(Set::stream).collect(Collectors.toSet());
    }
}
