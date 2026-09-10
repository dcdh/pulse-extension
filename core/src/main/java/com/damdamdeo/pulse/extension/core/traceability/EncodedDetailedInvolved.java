package com.damdamdeo.pulse.extension.core.traceability;

import java.util.Objects;

public record EncodedDetailedInvolved(TraceId traceId, EncodedInvolved encodedInvolved, From from, ExecutedAt executedAt) {

    public EncodedDetailedInvolved {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(encodedInvolved);
        Objects.requireNonNull(from);
        Objects.requireNonNull(executedAt);
    }
}
