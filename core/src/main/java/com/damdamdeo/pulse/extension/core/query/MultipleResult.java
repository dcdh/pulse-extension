package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record MultipleResult<A extends AggregateId, P extends Projection<A>>(List<P> projections)
        implements Result<A, P> {

    public MultipleResult {
        Objects.requireNonNull(projections);
    }

    @Override
    public Set<A> aggregateIds() {
        return projections.stream().map(Projection::id).collect(Collectors.toSet());
    }
}
