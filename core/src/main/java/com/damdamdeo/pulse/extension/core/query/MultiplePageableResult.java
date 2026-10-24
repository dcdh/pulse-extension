package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.pagination.Page;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record MultiplePageableResult<A extends AggregateId, P extends Projection<A>>(Page<P> projectionsParPage)
        implements Result<A, P> {

    public MultiplePageableResult {
        Objects.requireNonNull(projectionsParPage);
    }

    @Override
    public Set<A> aggregateIds() {
        return projectionsParPage.content()
                .stream()
                .map(Projection::id)
                .collect(Collectors.toSet());
    }

    @Override
    public List<P> projections() {
        return projectionsParPage.content();
    }

    public boolean hasNext() {
        return projectionsParPage.hasNext();
    }
}
