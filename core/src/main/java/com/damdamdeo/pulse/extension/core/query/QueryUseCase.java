package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.query.permission.Permission;

import java.util.List;

public interface QueryUseCase<A extends AggregateId, I extends Input, P extends Projection<A>, R extends Result<A, P>> {

    R execute(I input) throws QueryException;

    List<Permission<A>> permissions();
}
