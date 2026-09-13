package com.damdamdeo.pulse.extension.core.query;

import java.util.List;

public interface QueryUseCase<I extends Input, P extends Projection> {

    Result<P> execute(I input) throws QueryException;

    List<Audience> audiences();
}
