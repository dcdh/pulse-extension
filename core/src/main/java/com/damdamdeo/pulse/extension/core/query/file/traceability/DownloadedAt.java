package com.damdamdeo.pulse.extension.core.query.file.traceability;

import java.time.Instant;
import java.util.Objects;

public record DownloadedAt(Instant at) {

    public DownloadedAt {
        Objects.requireNonNull(at);
    }
}
