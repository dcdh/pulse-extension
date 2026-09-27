package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.traceability.Traceable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Result<A extends AggregateId, P extends Projection<A>>(List<P> projections,
                                                                     Set<A> aggregateIds) implements Traceable<A> {

    public Result {
        Objects.requireNonNull(projections);
        Objects.requireNonNull(aggregateIds);
    }

    public static <A extends AggregateId, P extends Projection<A>> Result<A, P> of(final List<P> projections, final Set<A> aggregateIds) {
        return new Result<>(projections, aggregateIds);
    }

    public static <A extends AggregateId, P extends Projection<A>> Result<A, P> of(final P projection, final Set<A> aggregateIds) {
        return new Result<>(List.of(projection), aggregateIds);
    }

    public P getFirst() {
        return projections.getFirst();
    }

    public int count() {
        return projections.size();
    }

    @Override
    public Set<A> aggregateIds() {
        return aggregateIds;
    }
}
