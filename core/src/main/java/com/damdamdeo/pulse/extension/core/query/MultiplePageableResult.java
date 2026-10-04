package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.pagination.Page;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record MultiplePageableResult<A extends AggregateId, P extends Projection<A>>(Page<P> projections)
        implements Result<A, P> {

    public MultiplePageableResult {
        Objects.requireNonNull(projections);
    }

    @Override
    public Set<A> aggregateIds() {
        return projections.content().stream().map(Projection::id).collect(Collectors.toSet());
    }

    public boolean hasNext() {
        return projections.hasNext();
    }
}
