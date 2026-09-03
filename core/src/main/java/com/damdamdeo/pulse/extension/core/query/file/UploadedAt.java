package com.damdamdeo.pulse.extension.core.query.file;

import java.time.Instant;
import java.util.Objects;

public record UploadedAt(Instant at) {

    public UploadedAt {
        Objects.requireNonNull(at);
    }
}
