package com.damdamdeo.pulse.extension.core.usecase.audience;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.Objects;

public final class Everyone implements Audience {

    public static final Everyone INSTANCE = new Everyone();

    private Everyone() {
    }

    @Override
    public boolean allow(final AggregateId aggregateId, final AudienceExecutionContext audienceExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(audienceExecutionContext);
        return allow(audienceExecutionContext);
    }

    @Override
    public boolean allow(final AudienceExecutionContext audienceExecutionContext) throws UseCaseException {
        Objects.requireNonNull(audienceExecutionContext);
        return true;
    }

    @Override
    public int priority() {
        return 0;
    }
}
