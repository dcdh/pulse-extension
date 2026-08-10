package com.damdamdeo.pulse.extension.core.query.file.traceability;

import java.util.UUID;

public final class DefaultTokenGenerator implements TokenGenerator {

    @Override
    public Token generate() {
        return new Token(UUID.randomUUID());
    }
}
