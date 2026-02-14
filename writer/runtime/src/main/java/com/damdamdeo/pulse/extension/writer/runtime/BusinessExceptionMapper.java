package com.damdamdeo.pulse.extension.writer.runtime;

import com.damdamdeo.pulse.extension.core.BusinessException;
import io.quarkiverse.resteasy.problem.ExceptionMapperBase;
import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Provider
@Priority(Priorities.USER)
@APIResponse(responseCode = "409", description = "BusinessException")
public class BusinessExceptionMapper extends ExceptionMapperBase<BusinessException>
        implements ExceptionMapper<BusinessException> {

    @Inject
    Instance<BusinessExceptionToHttpProblemDetailMapper> businessExceptionToHttpProblemDetailMapperInstance;

    @Override
    protected HttpProblem toProblem(final BusinessException exception) {
        final Response.Status status = Response.Status.CONFLICT;
        if (businessExceptionToHttpProblemDetailMapperInstance.isResolvable()) {
            return HttpProblem.valueOf(status, businessExceptionToHttpProblemDetailMapperInstance.get().toDetail(exception));
        } else {
            return HttpProblem.valueOf(status);
        }
    }
}
