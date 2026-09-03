package com.damdamdeo.pulse.extension.core.query.file;

import java.time.ZonedDateTime;

public class DefaultUploadedAtProvider implements UploadedAtProvider {

    @Override
    public UploadedAt now() {
        return new UploadedAt(ZonedDateTime.now());
    }
}
