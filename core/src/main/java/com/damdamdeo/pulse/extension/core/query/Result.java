package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.traceability.Traceable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record Result<A extends AggregateId, P extends Projection<A>>(List<P> projections) implements Traceable<A> {

    public Result {
        Objects.requireNonNull(projections);
    }

    public static <A extends AggregateId, P extends Projection<A>> Result<A, P> of(final List<P> projections) {
        return new Result<>(projections);
    }

    public static <A extends AggregateId, P extends Projection<A>> Result<A, P> of(final P projection) {
        return new Result<>(List.of(projection));
    }

    public P getFirst() {
        return projections.getFirst();
    }

    public int count() {
        return projections.size();
    }

    @Override
    public Set<A> aggregateIds() {
        return projections.stream().map(Projection::id).collect(Collectors.toSet());
    }
}
