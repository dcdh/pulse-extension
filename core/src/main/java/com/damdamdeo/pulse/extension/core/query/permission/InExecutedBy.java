package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.QueryExceptionCode;
import com.damdamdeo.pulse.extension.core.query.UnableToResolveException;

import java.util.Objects;
import java.util.Set;

public final class InExecutedBy<K extends AggregateId> implements Permission<K> {

    @Override
    public boolean allow(final Set<K> aggregateIds, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(aggregateIds);
        Objects.requireNonNull(permissionExecutionContext);
        try {
            final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
            final Set<AggregateId> uncompounded = permissionExecutionContext.aggregateIdDecomposer().unCompound(aggregateIds);
            final Set<ExecutedBy> executedByEligibles = permissionExecutionContext.executedByResolver().resolve(uncompounded);
            return executedByEligibles.contains(executionContext.executedBy());
        } catch (final UnableToResolveException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Override
    public int priority() {
        return 3;
    }
}
