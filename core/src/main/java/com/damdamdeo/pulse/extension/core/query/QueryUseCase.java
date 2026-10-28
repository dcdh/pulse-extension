package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.query.permission.Permission;
import com.damdamdeo.pulse.extension.core.traceability.TraceabilityDetails;

import java.util.List;

public interface QueryUseCase<A extends AggregateId, I extends Input, P extends Projection<A>, R extends Result<A, P>> {

    default R execute(final I input) throws QueryException {
        throw new UnsupportedOperationException();
    }

    default R execute(final I input, final Pagination pagination) throws QueryException {
        throw new UnsupportedOperationException();
    }

    List<Permission<A>> permissions();

    default TraceabilityDetails traceabilityDetails() {
        return TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY;
    }
}
