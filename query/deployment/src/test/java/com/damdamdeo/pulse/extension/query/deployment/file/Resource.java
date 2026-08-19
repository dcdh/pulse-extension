package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.core.query.file.ContentLength;

import java.io.InputStream;
import java.util.Objects;

public record Resource(InputStream payload, Long size) {

    public Resource {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(size, "size");
    }

    public ContentLength contentLength() {
        return new ContentLength(size);
    }
}
