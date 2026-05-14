package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public class SequenceGenerationException extends Exception {

    private final String sequenceName;

    public SequenceGenerationException(final String message, final String sequenceName) {
        super(message);
        this.sequenceName = Objects.requireNonNull(sequenceName);
    }

    public SequenceGenerationException(final String message, final Throwable cause, final String sequenceName) {
        super(message, cause);
        this.sequenceName = Objects.requireNonNull(sequenceName);
    }

    public String sequenceName() {
        return sequenceName;
    }
}
