package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.pagination.Page;

import java.util.Objects;

public record EncodedExecutedByInvolved(Page<EncodedDetailedInvolved> page) {

    public EncodedExecutedByInvolved {
        Objects.requireNonNull(page);
    }
}
