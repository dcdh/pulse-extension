package com.damdamdeo.pulse.extension.core.query.guard;

import com.damdamdeo.pulse.extension.core.query.QueryException;

@FunctionalInterface
public interface QueryCallable<T> {

    T execute() throws QueryException;
}
