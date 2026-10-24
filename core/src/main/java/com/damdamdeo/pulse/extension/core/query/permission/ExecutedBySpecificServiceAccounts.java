package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ExecutedBySpecificServiceAccounts<K extends AggregateId>(
        String... names) implements CompositePermission<K> {

    public ExecutedBySpecificServiceAccounts {
        Objects.requireNonNull(names);
    }

    @Override
    public boolean allow(final ProjectionId<K> projectionId, final PermissionExecutionContext permissionExecutionContext)
            throws QueryException {
        Objects.requireNonNull(projectionId);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        final Set<String> candidates = Stream.of(names).map(name -> ExecutedBy.ServiceAccount.DISCRIMINANT + ExecutedBy.SEPARATOR + name)
                .collect(Collectors.toSet());
        return candidates.contains(executionContext.executedBy().value());
    }
}
