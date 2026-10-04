package com.damdamdeo.pulse.extension.core.traceability;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record CorrelationId(Long id) {

    public static final CorrelationId NOT_AVAILABLE = new CorrelationId(0L);

    public CorrelationId {
        Objects.requireNonNull(id);
        Validate.isTrue(id >= 0);
    }
}
