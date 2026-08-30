package com.damdamdeo.pulse.extension.core.traceability;

import java.time.Instant;
import java.util.Objects;

public record ExecutedAt(Instant at) {

    public ExecutedAt {
        Objects.requireNonNull(at);
    }
}
