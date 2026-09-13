package com.damdamdeo.pulse.extension.core.query.audience;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.query.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RoleRestricted implements Audience {

    public static final RoleRestricted INSTANCE = new RoleRestricted();

    private RoleRestricted() {
    }

    @Override
    public <I extends Input, P extends Projection> Optional<Result<P>> execute(final I input, final QueryUseCase<I, P> decorated,
                                                                               final AudienceExecutionContext audienceExecutionContext) throws QueryException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(decorated);
        Objects.requireNonNull(audienceExecutionContext);
        final ExecutionContext executionContext = audienceExecutionContext.executionContextProvider().provide();
        final List<String> visibilityRoles = audienceExecutionContext.backendUserVisibilityRolesProvider().provide();
        if (visibilityRoles.stream().anyMatch(executionContext::hasRole)) {
            return Optional.of(decorated.execute(input));
        }
        return Optional.empty();
    }

    @Override
    public int priority() {
        return 1;
    }
}
