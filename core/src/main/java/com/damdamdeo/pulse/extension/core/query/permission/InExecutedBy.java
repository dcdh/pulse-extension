package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.*;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class InExecutedBy implements Permission {

    public static final InExecutedBy INSTANCE = new InExecutedBy();

    private InExecutedBy() {
    }

    @Override
    public <I extends Input, P extends Projection> Optional<Result<P>> execute(final I input, final QueryUseCase<I, P> decorated,
                                                                               final PermissionExecutionContext permissionExecutionContext)
            throws QueryException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(decorated);
        Objects.requireNonNull(permissionExecutionContext);
        try {
            final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
            final Result<P> executed = decorated.execute(input);
            final Set<AggregateId> uncompounded = permissionExecutionContext.aggregateIdDecomposer().unCompound(executed.aggregateIds());
            final Set<ExecutedBy> executedByEligibles = permissionExecutionContext.executedByResolver().resolve(uncompounded);
            if (executedByEligibles.contains(executionContext.executedBy())) {
                return Optional.of(executed);
            }
            return Optional.empty();
        } catch (final UnableToResolveException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Override
    public int priority() {
        return 2;
    }
}
