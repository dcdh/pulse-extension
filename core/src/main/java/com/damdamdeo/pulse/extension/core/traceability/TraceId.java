package com.damdamdeo.pulse.extension.core.traceability;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record TraceId(Long id) {

    public static final TraceId NOT_AVAILABLE = new TraceId(0L);

    public TraceId {
        Objects.requireNonNull(id);
        Validate.validState(id >= 0, "TraceId must be greater than or equal to 0");
    }
}
