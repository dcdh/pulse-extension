package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.traceability.Traceable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record Result<P extends Projection>(List<P> projections, Set<AggregateId> aggregateIds) implements Traceable {

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

    @Override
    public Set<AggregateId> aggregateIds() {
        return aggregateIds;
    }
}
