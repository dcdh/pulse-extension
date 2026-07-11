package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Result<P extends Projection>(List<P> projections, Set<AggregateId> aggregateIds) {

    public Result {
        Objects.requireNonNull(projections);
        Objects.requireNonNull(aggregateIds);
    }

    public static <P extends Projection> Result<P> of(List<P> projections, Set<AggregateId> aggregateIds) {
        return new Result<>(projections, aggregateIds);
    }

    public static <P extends Projection> Result<P> of(P projection, Set<AggregateId> aggregateIds) {
        return new Result<>(List.of(projection), aggregateIds);
    }

    public P getFirst() {
        return projections.getFirst();
    }

    public int count() {
        return projections.size();
    }

    public Set<AggregateId> aggregateIds() {
        return aggregateIds;
    }
}
