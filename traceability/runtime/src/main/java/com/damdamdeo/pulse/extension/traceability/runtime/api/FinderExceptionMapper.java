package com.damdamdeo.pulse.extension.traceability.runtime.api;

import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.traceability.FinderException;
import com.damdamdeo.pulse.extension.core.traceability.OwnedByProviderException;
import com.damdamdeo.pulse.extension.core.traceability.TraceRepositoryException;
import io.quarkiverse.resteasy.problem.ExceptionMapperBase;
import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER)
public class FinderExceptionMapper extends ExceptionMapperBase<FinderException> implements ExceptionMapper<FinderException> {

    @Override
    protected HttpProblem toProblem(final FinderException exception) {
        return switch (exception.getCause()) {
            case TraceRepositoryException e -> HttpProblem.valueOf(Response.Status.INTERNAL_SERVER_ERROR);
            case OwnedByProviderException e -> HttpProblem.valueOf(Response.Status.INTERNAL_SERVER_ERROR);
            case UnauthorizedException e -> HttpProblem.valueOf(Response.Status.FORBIDDEN);
            default -> HttpProblem.valueOf(Response.Status.INTERNAL_SERVER_ERROR);
        };
    }
}
