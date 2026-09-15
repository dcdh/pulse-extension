package com.damdamdeo.pulse.extension.core.query.audience;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.query.*;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public record ExecutedByHasAtLeastOneRole(String... roleNames) implements Audience {

    public ExecutedByHasAtLeastOneRole {
        Objects.requireNonNull(roleNames);
    }

    @Override
    public <I extends Input, P extends Projection> Optional<Result<P>> execute(final I input, final QueryUseCase<I, P> decorated,
                                                                               final AudienceExecutionContext audienceExecutionContext)
            throws QueryException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(decorated);
        Objects.requireNonNull(audienceExecutionContext);
        final ExecutionContext executionContext = audienceExecutionContext.executionContextProvider().provide();
        if (Arrays.stream(roleNames).anyMatch(executionContext::hasRole)) {
            return Optional.of(decorated.execute(input));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public int priority() {
        return 4;
    }
}
