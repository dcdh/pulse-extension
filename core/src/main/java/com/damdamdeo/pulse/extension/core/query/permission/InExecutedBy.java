package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.QueryExceptionCode;
import com.damdamdeo.pulse.extension.core.query.UnableToResolveException;

import java.util.Objects;
import java.util.Set;

public final class InExecutedBy<K extends Identifiable> implements CompositePermission<K> {

    @Override
    public boolean allow(final ProjectionId<K> projectionId, final PermissionExecutionContext permissionExecutionContext)
            throws QueryException {
        Objects.requireNonNull(projectionId);
        Objects.requireNonNull(permissionExecutionContext);
        try {
            final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
            final Set<AggregateId> uncompounded = permissionExecutionContext.aggregateIdDecomposer().unCompound(
                    AnyAggregateId.from(projectionId.linkTo()));
            final Set<ExecutedBy> executedByEligibles = permissionExecutionContext.executedByResolver().resolve(uncompounded);
            return executedByEligibles.contains(executionContext.executedBy());
        } catch (final UnableToResolveException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }
}
