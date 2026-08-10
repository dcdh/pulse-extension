package com.damdamdeo.pulse.extension.core.query.file.traceability;

import java.util.Objects;
import java.util.UUID;

public record Token(UUID value) {

    public Token {
        Objects.requireNonNull(value);
    }
}
