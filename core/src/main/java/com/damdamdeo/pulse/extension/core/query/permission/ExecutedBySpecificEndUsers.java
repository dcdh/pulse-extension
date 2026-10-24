package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.Objects;
import java.util.stream.Stream;

public record ExecutedBySpecificEndUsers<K extends AggregateId>(
        ExecutedBy.EndUser... endUsers) implements Permission<K> {

    public ExecutedBySpecificEndUsers {
        Objects.requireNonNull(endUsers);
    }

    @Override
    public boolean allow(final K aggregateId, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        final ExecutedBy executedBy = executionContext.executedBy();
        return Stream.of(endUsers).anyMatch(endUser -> endUser.value().equals(executedBy.value()));
    }
}
