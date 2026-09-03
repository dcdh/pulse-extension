package com.damdamdeo.pulse.extension.core.query.file.traceability;

import java.time.Instant;

public final class DefaultDownloadedAtProvider implements DownloadedAtProvider {

    @Override
    public DownloadedAt now() {
        return new DownloadedAt(Instant.now());
    }
}
