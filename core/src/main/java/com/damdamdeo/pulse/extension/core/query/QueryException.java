package com.damdamdeo.pulse.extension.core.query;

import java.util.Objects;

public class QueryException extends Exception {

    private final QueryExceptionCode queryExceptionCode;

    public QueryException(final UnauthorizedException exception) {
        this(exception, QueryExceptionCode.FORBIDDEN);
    }

    public QueryException(final Throwable cause, final QueryExceptionCode queryExceptionCode) {
        super(cause);
        this.queryExceptionCode = Objects.requireNonNull(queryExceptionCode);
    }

    public QueryExceptionCode queryExceptionCode() {
        return queryExceptionCode;
    }
}
