package com.damdamdeo.pulse.extension.core.query;

import java.util.List;

public interface Query<I, P extends Projection, R extends Result<P>> {

    R execute(I input) throws QueryException;

    List<Audience> audiences();
}
