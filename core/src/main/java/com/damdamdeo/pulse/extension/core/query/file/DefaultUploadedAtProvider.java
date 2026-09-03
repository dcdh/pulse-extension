package com.damdamdeo.pulse.extension.core.query.file;

import java.time.Instant;

public class DefaultUploadedAtProvider implements UploadedAtProvider {

    @Override
    public UploadedAt now() {
        return new UploadedAt(Instant.now());
    }
}
