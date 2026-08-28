package com.damdamdeo.pulse.extension.core.query.file.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.Objects;

public record DownloadedBy(ExecutedBy executedBy) {

    public DownloadedBy {
        Objects.requireNonNull(executedBy);
    }
}
