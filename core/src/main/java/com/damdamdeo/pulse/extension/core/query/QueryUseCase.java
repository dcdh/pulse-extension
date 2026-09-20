package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.query.permission.Permission;

import java.util.List;

public interface QueryUseCase<I extends Input, P extends Projection> {

    Result<P> execute(I input) throws QueryException;

    List<Permission> permissions();
}
