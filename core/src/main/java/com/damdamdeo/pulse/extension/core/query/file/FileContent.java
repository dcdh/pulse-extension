package com.damdamdeo.pulse.extension.core.query.file;

import java.io.InputStream;
import java.util.Objects;

public record FileContent(FileIdentifier id,
                          ContentType contentType,
                          ContentLength contentLength,
                          InputStream content) {

    public FileContent {
        Objects.requireNonNull(id);
        Objects.requireNonNull(contentType);
        Objects.requireNonNull(contentLength);
        Objects.requireNonNull(content);
    }
}
