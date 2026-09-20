package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.UnableToResolveException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseExceptionCode;

import java.util.Objects;
import java.util.Set;

public final class InExecutedBy implements Permission {

    public static final InExecutedBy INSTANCE = new InExecutedBy();

    private InExecutedBy() {
    }

    @Override
    public boolean allow(final AggregateId aggregateId, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(permissionExecutionContext);
        try {
            final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
            final Set<AggregateId> uncompounded = permissionExecutionContext.aggregateIdDecomposer().unCompound(Set.of(aggregateId));
            final Set<ExecutedBy> executedByEligibles = permissionExecutionContext.executedByResolver().resolve(uncompounded);
            return executedByEligibles.contains(executionContext.executedBy());
        } catch (final UnableToResolveException exception) {
            throw new UseCaseException(exception, UseCaseExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Override
    public boolean allow(final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(permissionExecutionContext);
        return false;
    }

    @Override
    public int priority() {
        return 3;
    }
}
