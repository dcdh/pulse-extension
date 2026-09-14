package com.damdamdeo.pulse.extension.core.query.audience;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.*;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public record ExecutedBySpecificEndUsers(ExecutedBy.EndUser... endUsers) implements Audience {

    public ExecutedBySpecificEndUsers {
        Objects.requireNonNull(endUsers);
    }

    @Override
    public <I extends Input, P extends Projection> Optional<Result<P>> execute(final I input, final QueryUseCase<I, P> decorated,
                                                                               final AudienceExecutionContext audienceExecutionContext)
            throws QueryException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(decorated);
        Objects.requireNonNull(audienceExecutionContext);
        final ExecutionContext executionContext = audienceExecutionContext.executionContextProvider().provide();
        final ExecutedBy executedBy = executionContext.executedBy();
        if (Stream.of(endUsers).anyMatch(endUser -> endUser.value().equals(executedBy.value()))) {
            return Optional.of(decorated.execute(input));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public int priority() {
        return 5;
    }
}
