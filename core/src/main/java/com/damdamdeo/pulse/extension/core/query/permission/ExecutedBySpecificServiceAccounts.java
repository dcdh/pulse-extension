package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.*;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ExecutedBySpecificServiceAccounts(String... names) implements Permission {

    public ExecutedBySpecificServiceAccounts {
        Objects.requireNonNull(names);
    }

    @Override
    public <I extends Input, P extends Projection> Optional<Result<P>> execute(final I input, final QueryUseCase<I, P> decorated,
                                                                               final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(decorated);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        final Set<String> candidates = Stream.of(names).map(name -> ExecutedBy.ServiceAccount.DISCRIMINANT + ExecutedBy.SEPARATOR + name)
                .collect(Collectors.toSet());
        if (candidates.contains(executionContext.executedBy().value())) {
            return Optional.of(decorated.execute(input));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public int priority() {
        return 3;
    }
}
