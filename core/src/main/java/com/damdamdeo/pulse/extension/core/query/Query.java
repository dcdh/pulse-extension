package com.damdamdeo.pulse.extension.core.query;

import java.util.List;

public interface Query<I extends Input, P extends Projection> {

    Result<P> execute(I input) throws QueryException;

    List<Audience> audiences();
}
