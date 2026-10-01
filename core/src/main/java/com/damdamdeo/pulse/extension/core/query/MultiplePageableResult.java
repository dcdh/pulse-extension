package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record MultiplePageableResult<A extends AggregateId, P extends Projection<A>>(List<P> projections,
                                                                                     Pagination pagination,
                                                                                     Long totalElements)
        implements Result<A, P> {

    public MultiplePageableResult {
        Objects.requireNonNull(projections);
        Objects.requireNonNull(pagination);
        Objects.requireNonNull(totalElements);
    }

    @Override
    public Set<A> aggregateIds() {
        return projections.stream().map(Projection::id).collect(Collectors.toSet());
    }
}
