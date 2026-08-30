package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;

import java.util.Objects;

public record TracedByHashed(String hashed) {

    public TracedByHashed {
        Objects.requireNonNull(hashed);
    }

    public static TracedByHashed from(final ExecutedByHashed executedByHashed) {
        Objects.requireNonNull(executedByHashed);
        return new TracedByHashed(executedByHashed.hashed());
    }
}
