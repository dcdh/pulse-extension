package com.damdamdeo.pulse.extension.core.query.file.query;

import java.util.Map;
import java.util.Objects;

public record CustomMetadata(Map<String, String> metadata) {

    public CustomMetadata {
        Objects.requireNonNull(metadata);
    }
}
