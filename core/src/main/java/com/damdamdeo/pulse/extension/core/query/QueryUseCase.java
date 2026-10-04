package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.query.permission.Permission;

import java.util.List;

public interface QueryUseCase<A extends AggregateId, I extends Input, P extends Projection<A>, R extends Result<A, P>> {

    default R execute(final I input) throws QueryException {
        throw new UnsupportedOperationException();
    }

    default R execute(final I input, final Pagination pagination) throws QueryException {
        throw new UnsupportedOperationException();
    }

    List<Permission<A>> permissions();
}
