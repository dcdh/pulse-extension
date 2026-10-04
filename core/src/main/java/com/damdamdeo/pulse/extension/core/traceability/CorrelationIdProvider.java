package com.damdamdeo.pulse.extension.core.traceability;

public interface CorrelationIdProvider {

    CorrelationId provide() throws CorrelationIdProviderException;
}
