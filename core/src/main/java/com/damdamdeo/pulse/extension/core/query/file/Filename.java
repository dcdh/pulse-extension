package com.damdamdeo.pulse.extension.core.query.file;

import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.stream.Stream;

public record Filename(String filename) {

    public Filename {
        Objects.requireNonNull(filename);
        Validate.validState(!filename.isEmpty() && filename.length() <= 1024);
    }

    public ContentType contentType() {
        return Stream.of(ContentType.values())
                .filter(contentType -> filename.endsWith("." + contentType.extension()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown content type for " + filename));
    }
}
