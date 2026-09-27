package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.Prioritable;
import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.permission.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.permission.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.permission.Permission;
import com.damdamdeo.pulse.extension.core.traceability.*;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public abstract class GuardQueryUseCase<I extends Input, P extends Projection> implements QueryUseCase<I, P> {

    private final ExecutionContextProvider executionContextProvider;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;
    private final ExecutedByResolver executedByResolver;
    private final AggregateIdDecomposer aggregateIdDecomposer;
    private final QueryUseCase<I, P> decorated;
    private final TraceAppender traceAppender;

    public GuardQueryUseCase(final ExecutionContextProvider executionContextProvider,
                             final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                             final ExecutedByResolver executedByResolver,
                             final QueryUseCase<I, P> decorated,
                             final TraceAppender traceAppender) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
        this.executedByResolver = Objects.requireNonNull(executedByResolver);
        this.aggregateIdDecomposer = new AggregateIdDecomposer();
        this.decorated = Objects.requireNonNull(decorated);
        this.traceAppender = Objects.requireNonNull(traceAppender);
    }

    @Override
    public final Result<P> execute(final I input) throws QueryException {
        Objects.requireNonNull(input);
        final List<Permission> permissions = decorated.permissions()
                .stream()
                .sorted(Comparator.comparing(Prioritable::priority))
                .toList();
        final PermissionExecutionContext context = new PermissionExecutionContext(executionContextProvider,
                backendUserVisibilityRolesProvider, executedByResolver, aggregateIdDecomposer);
        try {
            final Result<P> result = decorated.execute(input);
            boolean allow = false;
            for (final Permission permission : permissions) {
                if (permission.allow(result.aggregateIds(), context)) {
                    allow = true;
                    break;
                }
            }
            if (allow) {
                traceAppender.append(result, Source.QUERY, ExecutionStatus.SUCCESS, From.from(input));
                return result;
            } else {
                traceAppender.append(result, Source.QUERY, ExecutionStatus.FAILED_UNAUTHORIZED, From.from(input));
                throw new QueryException(new UnauthorizedException());
            }
        } catch (final TraceAppenderException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Override
    public List<Permission> permissions() {
        return decorated.permissions();
    }
}
