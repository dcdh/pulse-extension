package com.damdamdeo.pulse.extension.core.traceability;

import java.util.Objects;

public record IncludeUncompounded(Boolean included) {

    public IncludeUncompounded {
        Objects.requireNonNull(included);
    }
}
