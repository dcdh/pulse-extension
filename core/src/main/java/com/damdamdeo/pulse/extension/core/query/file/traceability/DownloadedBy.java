package com.damdamdeo.pulse.extension.core.query.file.traceability;

import java.util.Objects;

public record DownloadedBy(String by) {

    public DownloadedBy {
        Objects.requireNonNull(by);
    }
}
