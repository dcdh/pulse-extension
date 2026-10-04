package com.damdamdeo.pulse.extension.core.traceability;

public final class NoOpCorrelationIdProvider implements CorrelationIdProvider {

    @Override
    public CorrelationId provide() {
        return CorrelationId.NOT_AVAILABLE;
    }
}
