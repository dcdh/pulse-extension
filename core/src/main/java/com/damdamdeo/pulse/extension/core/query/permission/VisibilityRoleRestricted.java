package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class VisibilityRoleRestricted implements Permission {

    public static final VisibilityRoleRestricted INSTANCE = new VisibilityRoleRestricted();

    private VisibilityRoleRestricted() {
    }

    @Override
    public <I extends Input, P extends Projection> Optional<Result<P>> execute(final I input, final QueryUseCase<I, P> decorated,
                                                                               final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(decorated);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        final List<String> visibilityRoles = permissionExecutionContext.backendUserVisibilityRolesProvider().provide();
        if (visibilityRoles.stream().anyMatch(executionContext::hasRole)) {
            return Optional.of(decorated.execute(input));
        }
        return Optional.empty();
    }

    @Override
    public int priority() {
        return 2;
    }
}
