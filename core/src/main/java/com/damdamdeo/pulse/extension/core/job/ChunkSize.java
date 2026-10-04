package com.damdamdeo.pulse.extension.core.job;

import org.apache.commons.lang3.Validate;

public record ChunkSize(int size) {

    public static final ChunkSize ALL = new ChunkSize(-1);

    public ChunkSize {
        Validate.isTrue(size >= -1, "size must be greater than or equal to -1");
    }
}
