package com.damdamdeo.pulse.extension.core.query;

public interface Query<I, P extends Projection> {

    Result<P> execute(I input) throws QueryException;
}
