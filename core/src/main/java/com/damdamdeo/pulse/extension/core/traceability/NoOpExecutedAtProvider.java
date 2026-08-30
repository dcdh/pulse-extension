package com.damdamdeo.pulse.extension.core.traceability;

public final class NoOpExecutedAtProvider implements ExecutedAtProvider {

    @Override
    public ExecutedAt now() {
        throw new UnsupportedOperationException("No-op executed at provider");
    }
}
