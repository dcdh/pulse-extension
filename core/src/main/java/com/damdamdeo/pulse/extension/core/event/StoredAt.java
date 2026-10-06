package com.damdamdeo.pulse.extension.core.event;

import java.time.Instant;
import java.util.Objects;

public record StoredAt(Instant at) {

    public StoredAt {
        Objects.requireNonNull(at);
    }
}
