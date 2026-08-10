package com.damdamdeo.pulse.extension.core.query.file.traceability;

import java.time.ZonedDateTime;
import java.util.Objects;

public record DownloadedAt(ZonedDateTime at) {

    public DownloadedAt {
        Objects.requireNonNull(at);
    }
}
