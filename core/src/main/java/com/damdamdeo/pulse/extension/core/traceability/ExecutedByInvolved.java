package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.pagination.Page;

import java.util.Objects;

public record ExecutedByInvolved(Page<DetailedInvolved> page) {

    public ExecutedByInvolved {
        Objects.requireNonNull(page);
    }
}
