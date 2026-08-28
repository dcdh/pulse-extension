package com.damdamdeo.pulse.extension.core.query.file;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record FileMetadata(Map<String, List<String>> metadata) {

    public FileMetadata {
        Objects.requireNonNull(metadata);
    }

    public Map<String, List<String>> metadata() {
        return Collections.unmodifiableMap(metadata);
    }
}
