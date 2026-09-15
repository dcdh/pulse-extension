package com.damdamdeo.pulse.extension.core.usecase.audience;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.List;
import java.util.Objects;

public final class VisibilityRoleRestricted implements Audience {

    public static final VisibilityRoleRestricted INSTANCE = new VisibilityRoleRestricted();

    private VisibilityRoleRestricted() {
    }

    @Override
    public boolean allow(final AggregateId aggregateId, final AudienceExecutionContext audienceExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        return allow(audienceExecutionContext);
    }

    @Override
    public boolean allow(final AudienceExecutionContext audienceExecutionContext) throws UseCaseException {
        Objects.requireNonNull(audienceExecutionContext);
        final ExecutionContext executionContext = audienceExecutionContext.executionContextProvider().provide();
        final List<String> visibilityRoles = audienceExecutionContext.backendUserVisibilityRolesProvider().provide();
        return visibilityRoles.stream().anyMatch(executionContext::hasRole);
    }

    @Override
    public int priority() {
        return 1;
    }
}
