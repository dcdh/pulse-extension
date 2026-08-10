package com.damdamdeo.pulse.extension.core.query;

public interface GenericQuery<I, P> {

    P execute(I input) throws QueryException;
}
