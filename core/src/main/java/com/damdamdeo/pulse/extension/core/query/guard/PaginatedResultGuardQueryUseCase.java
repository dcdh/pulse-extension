package com.damdamdeo.pulse.extension.core.query.guard;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.permission.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.permission.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppender;

import java.util.Objects;

public abstract class PaginatedResultGuardQueryUseCase<A extends AggregateId, I extends Input, P extends Projection<A>, R extends MultiplePageableResult<A, P>>
        extends GuardQueryUseCase<A, I, P, R> implements PaginatedResultQueryUseCase<A, I, P, R> {

    final PaginatedResultQueryUseCase<A, I, P, R> decorated;

    public PaginatedResultGuardQueryUseCase(final ExecutionContextProvider executionContextProvider,
                                            final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                                            final ExecutedByResolver executedByResolver,
                                            final PaginatedResultQueryUseCase<A, I, P, R> decorated, TraceAppender traceAppender) {
        super(executionContextProvider, backendUserVisibilityRolesProvider, executedByResolver, decorated, traceAppender);
        this.decorated = Objects.requireNonNull(decorated);
    }

    @Override
    public final R execute(final I input, final Pagination pagination) throws QueryException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(pagination);
        return internalExecution(new QueryCallable<R>() {
            @Override
            public R execute() throws QueryException {
                return decorated.execute(input, pagination);
            }
        }, input);
    }
}
