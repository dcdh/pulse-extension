package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.query.QueryException;
import io.quarkiverse.resteasy.problem.ExceptionMapperBase;
import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER)
public class QueryExceptionMapper extends ExceptionMapperBase<QueryException> implements ExceptionMapper<QueryException> {

    @Override
    protected HttpProblem toProblem(final QueryException exception) {
        final Response.Status status = switch (exception.queryExceptionCode()) {
            case UNKNOWN -> Response.Status.NOT_FOUND;
            case FORBIDDEN -> Response.Status.FORBIDDEN;
            case CONFLICT -> Response.Status.CONFLICT;
            case FAIL_FAST_CONDITION_NOT_MET -> Response.Status.BAD_REQUEST;
            case INFRASTRUCTURE_FAILURE -> Response.Status.INTERNAL_SERVER_ERROR;
        };
        return HttpProblem.valueOf(status, exception.getMessage());
    }
}
