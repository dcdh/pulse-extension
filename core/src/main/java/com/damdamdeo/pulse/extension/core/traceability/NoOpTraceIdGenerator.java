package com.damdamdeo.pulse.extension.core.traceability;

public final class NoOpTraceIdGenerator implements TraceIdGenerator {

    @Override
    public TraceId generate() throws TraceIdGeneratorException {
        return TraceId.NOT_AVAILABLE;
    }
}
