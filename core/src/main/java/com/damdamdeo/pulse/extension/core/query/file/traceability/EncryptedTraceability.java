package com.damdamdeo.pulse.extension.core.query.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;

import java.util.Objects;

public record EncryptedTraceability(Token token, FileIdentifier fileIdentifier,
                                    EncryptedDownloadedBy encryptedDownloadedBy,
                                    DownloadedAt downloadedAt) {

    public EncryptedTraceability {
        Objects.requireNonNull(token);
        Objects.requireNonNull(fileIdentifier);
        Objects.requireNonNull(encryptedDownloadedBy);
        Objects.requireNonNull(downloadedAt);
    }
}
