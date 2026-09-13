package com.damdamdeo.pulse.extension.core.query.audience;

import com.damdamdeo.pulse.extension.core.query.*;

import java.util.Objects;
import java.util.Optional;

public final class Everyone implements Audience {

    public static final Everyone INSTANCE = new Everyone();

    private Everyone() {
    }

    @Override
    public <I extends Input, P extends Projection> Optional<Result<P>> execute(final I input, final QueryUseCase<I, P> decorated,
                                                                               final AudienceExecutionContext audienceExecutionContext) throws QueryException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(decorated);
        Objects.requireNonNull(audienceExecutionContext);
        return Optional.of(decorated.execute(input));
    }

    @Override
    public int priority() {
        return 0;
    }
}
