package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record SingleResult<A extends AggregateId, P extends Projection<A>>(P projection) implements Result<A, P> {

    public SingleResult {
        Objects.requireNonNull(projection);
    }

    @Override
    public Set<ProjectionId<A>> projectionIds() {
        return Set.of(projection.id());
    }

    @Override
    public List<P> projections() {
        return List.of(projection);
    }
}
