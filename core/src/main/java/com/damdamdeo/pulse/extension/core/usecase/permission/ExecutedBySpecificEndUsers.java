package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.Objects;
import java.util.stream.Stream;

public record ExecutedBySpecificEndUsers(ExecutedBy.EndUser... endUsers) implements Permission {

    public ExecutedBySpecificEndUsers {
        Objects.requireNonNull(endUsers);
    }

    @Override
    public boolean allow(final AggregateId aggregateId, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(permissionExecutionContext);
        return allow(permissionExecutionContext);
    }

    @Override
    public boolean allow(final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        final ExecutedBy executedBy = executionContext.executedBy();
        return Stream.of(endUsers).anyMatch(endUser -> endUser.value().equals(executedBy.value()));
    }

    @Override
    public int priority() {
        return 5;
    }
}
