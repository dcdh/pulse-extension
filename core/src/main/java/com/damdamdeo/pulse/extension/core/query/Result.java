package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.traceability.Traceable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record Result<A extends AggregateId, P extends Projection<A>>(Page<P> projections) implements Traceable<A> {

    public Result {
        Objects.requireNonNull(projections);
    }

    public static <A extends AggregateId, P extends Projection<A>> Result<A, P> of(final List<P> projections,
                                                                                   final Pagination pagination,
                                                                                   final Long totalElements) {
        Objects.requireNonNull(projections);
        Objects.requireNonNull(pagination);
        Objects.requireNonNull(totalElements);
        return new Result<>(new Page<>(projections, pagination, totalElements));
    }

    public static <A extends AggregateId, P extends Projection<A>> Result<A, P> of(final List<P> projections) {
        return new Result<>(new Page<>(projections, new Pagination(0, projections.size()), projections.size()));
    }

    public static <A extends AggregateId, P extends Projection<A>> Result<A, P> of(final P projection) {
        return new Result<>(new Page<>(List.of(projection), new Pagination(0, 1), 1L));
    }

    public P getFirst() {
        return projections.content().getFirst();
    }

    @Override
    public Set<A> aggregateIds() {
        return projections.content().stream().map(Projection::id).collect(Collectors.toSet());
    }
}
