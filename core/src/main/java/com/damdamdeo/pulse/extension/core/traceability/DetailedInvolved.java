package com.damdamdeo.pulse.extension.core.traceability;

import java.util.Objects;

public record DetailedInvolved(TraceId traceId, Involved encodedInvolved, From from, ExecutedAt executedAt) {

    public DetailedInvolved {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(encodedInvolved);
        Objects.requireNonNull(from);
        Objects.requireNonNull(executedAt);
    }
}
