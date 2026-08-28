package com.damdamdeo.pulse.extension.core.query.file;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record CustomMetadata(Map<String, String> metadata) {

    public CustomMetadata {
        Objects.requireNonNull(metadata);
    }

    public Map<String, String> metadata() {
        return Collections.unmodifiableMap(metadata);
    }
}
