package com.damdamdeo.pulse.extension.core.query.guard;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.permission.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.permission.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppender;

import java.util.Objects;

public abstract class SingleResultGuardQueryUseCase<A extends AggregateId, I extends Input, P extends Projection<A>, R extends SingleResult<A, P>> extends GuardQueryUseCase<A, I, P, R>
        implements SingleResultQueryUseCase<A, I, P, R> {

    final SingleResultQueryUseCase<A, I, P, R> decorated;

    public SingleResultGuardQueryUseCase(final ExecutionContextProvider executionContextProvider,
                                         final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                                         final ExecutedByResolver executedByResolver,
                                         final SingleResultQueryUseCase<A, I, P, R> decorated, TraceAppender traceAppender) {
        super(executionContextProvider, backendUserVisibilityRolesProvider, executedByResolver, decorated, traceAppender);
        this.decorated = Objects.requireNonNull(decorated);
    }

    @Override
    public final R execute(final I input) throws QueryException {
        Objects.requireNonNull(input);
        return internalExecution(new QueryCallable<R>() {

            @Override
            public R execute() throws QueryException {
                return decorated.execute(input);
            }
        }, input);
    }
}

