package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.Prioritable;
import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.permission.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.permission.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.permission.Permission;
import com.damdamdeo.pulse.extension.core.traceability.ExecutionStatus;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppender;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppenderException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public abstract class GuardQueryUseCase<A extends AggregateId, I extends Input, P extends Projection<A>, R extends Result<A, P>> implements QueryUseCase<A, I, P, R> {

    private final ExecutionContextProvider executionContextProvider;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;
    private final ExecutedByResolver executedByResolver;
    private final AggregateIdDecomposer aggregateIdDecomposer;
    private final QueryUseCase<A, I, P, R> decorated;
    private final TraceAppender traceAppender;

    public GuardQueryUseCase(final ExecutionContextProvider executionContextProvider,
                             final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                             final ExecutedByResolver executedByResolver,
                             final QueryUseCase<A, I, P, R> decorated,
                             final TraceAppender traceAppender) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
        this.executedByResolver = Objects.requireNonNull(executedByResolver);
        this.aggregateIdDecomposer = new AggregateIdDecomposer();
        this.decorated = Objects.requireNonNull(decorated);
        this.traceAppender = Objects.requireNonNull(traceAppender);
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

    @FunctionalInterface
    public interface QueryCallable<T> {

        T execute() throws QueryException;
    }

    private R internalExecution(final QueryCallable<R> queryCallable, final Input input) throws QueryException {
        Objects.requireNonNull(queryCallable);
        Objects.requireNonNull(input);
        final List<Permission<A>> permissions = decorated.permissions()
                .stream()
                .sorted(Comparator.comparing(Prioritable::priority))
                .toList();
        final PermissionExecutionContext context = new PermissionExecutionContext(executionContextProvider,
                backendUserVisibilityRolesProvider, executedByResolver, aggregateIdDecomposer);
        try {
            final R result = queryCallable.execute();
            boolean allow = false;
            for (final Permission<A> permission : permissions) {
                if (permission.allow(result.aggregateIds(), context)) {
                    allow = true;
                    break;
                }
            }
            if (allow) {
                traceAppender.append(input, result.aggregateIds(), ExecutionStatus.SUCCESS);
                return result;
            } else {
                traceAppender.append(input, result.aggregateIds(), ExecutionStatus.FAILED_UNAUTHORIZED);
                throw new QueryException(new UnauthorizedException());
            }
        } catch (final TraceAppenderException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Override
    public List<Permission<A>> permissions() {
        return decorated.permissions();
    }
}
