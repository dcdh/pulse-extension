package com.damdamdeo.pulse.extension.core.traceability;

public interface CorrelationIdGenerator {

    CorrelationId generate() throws CorrelationIdGeneratorException;
}
