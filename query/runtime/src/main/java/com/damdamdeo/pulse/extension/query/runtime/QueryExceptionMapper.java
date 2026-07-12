package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.query.QueryException;
import io.quarkiverse.resteasy.problem.ExceptionMapperBase;
import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Provider
@Priority(Priorities.USER)
@APIResponse(responseCode = "409", description = "QueryException")
public class QueryExceptionMapper extends ExceptionMapperBase<QueryException>
        implements ExceptionMapper<QueryException> {

    @Override
    protected HttpProblem toProblem(final QueryException exception) {
        return HttpProblem.valueOf(Response.Status.CONFLICT);
    }
}
