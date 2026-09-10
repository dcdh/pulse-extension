package com.damdamdeo.pulse.extension.core.traceability;

import java.util.Objects;

public record DetailedInvolved(TraceId traceId, Involved involved, From from, ExecutedAt executedAt) {

    public DetailedInvolved {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(involved);
        Objects.requireNonNull(from);
        Objects.requireNonNull(executedAt);
    }
}
