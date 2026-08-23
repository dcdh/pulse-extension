package com.damdamdeo.pulse.extension.query.deployment.file;

import java.util.Objects;

public record TokenDownload(String token, String fileIdentifier, String downloadedBy, String downloadedAt) {

    public TokenDownload {
        Objects.requireNonNull(token);
        Objects.requireNonNull(fileIdentifier);
        Objects.requireNonNull(downloadedBy);
        Objects.requireNonNull(downloadedAt);
    }
}
