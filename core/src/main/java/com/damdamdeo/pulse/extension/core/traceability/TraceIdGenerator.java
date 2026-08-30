package com.damdamdeo.pulse.extension.core.traceability;

public interface TraceIdGenerator {

    TraceId generate() throws TraceIdGeneratorException;
}
