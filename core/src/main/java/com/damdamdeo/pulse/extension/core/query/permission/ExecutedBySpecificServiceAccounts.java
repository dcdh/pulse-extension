package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ExecutedBySpecificServiceAccounts(String... names) implements Permission {

    public ExecutedBySpecificServiceAccounts {
        Objects.requireNonNull(names);
    }

    @Override
    public boolean allow(final Set<AggregateId> aggregateIds, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(aggregateIds);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        final Set<String> candidates = Stream.of(names).map(name -> ExecutedBy.ServiceAccount.DISCRIMINANT + ExecutedBy.SEPARATOR + name)
                .collect(Collectors.toSet());
        return candidates.contains(executionContext.executedBy().value());
    }

    @Override
    public int priority() {
        return 4;
    }
}
