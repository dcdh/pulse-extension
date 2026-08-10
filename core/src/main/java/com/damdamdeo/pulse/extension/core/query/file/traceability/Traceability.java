package com.damdamdeo.pulse.extension.core.query.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;

import java.util.Objects;

public record Traceability(Token token, FileIdentifier fileIdentifier, DownloadedBy downloadedBy,
                           DownloadedAt downloadedAt) {

    public Traceability {
        Objects.requireNonNull(token);
        Objects.requireNonNull(fileIdentifier);
        Objects.requireNonNull(downloadedBy);
        Objects.requireNonNull(downloadedAt);
    }
}
