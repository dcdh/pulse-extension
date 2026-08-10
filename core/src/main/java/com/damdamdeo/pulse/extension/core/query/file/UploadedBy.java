package com.damdamdeo.pulse.extension.core.query.file;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.Objects;

public record UploadedBy(ExecutedBy executedBy) {

    public UploadedBy {
        Objects.requireNonNull(executedBy);
    }
}
