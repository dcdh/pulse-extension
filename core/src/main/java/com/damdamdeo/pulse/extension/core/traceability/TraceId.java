package com.damdamdeo.pulse.extension.core.traceability;

import java.util.Objects;

public record TraceId(Long id) {

    public TraceId {
        Objects.requireNonNull(id);
    }
}
