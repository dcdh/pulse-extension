package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;

public interface PaginatedResultQueryUseCase<A extends AggregateId, I extends Input, P extends Projection<A>, R extends MultiplePageableResult<A, P>>
        extends QueryUseCase<A, I, P, R> {

    R execute(final I input, final Pagination pagination) throws QueryException;
}
