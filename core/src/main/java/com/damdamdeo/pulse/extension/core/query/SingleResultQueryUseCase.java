package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;

public interface SingleResultQueryUseCase<A extends AggregateId, I extends Input, P extends Projection<A>, R extends SingleResult<A, P>>
        extends QueryUseCase<A, I, P, R> {

    R execute(final I input) throws QueryException;
}
