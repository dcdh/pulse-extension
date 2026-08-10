package com.damdamdeo.pulse.extension.core.query.file.traceability;

import java.time.ZonedDateTime;

public final class DefaultDownloadedAtProvider implements DownloadedAtProvider {

    @Override
    public DownloadedAt provide() {
        return new DownloadedAt(ZonedDateTime.now());
    }
}
