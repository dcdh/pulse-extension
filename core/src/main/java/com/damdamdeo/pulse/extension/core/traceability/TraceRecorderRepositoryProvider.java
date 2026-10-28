package com.damdamdeo.pulse.extension.core.traceability;

public interface TraceRecorderRepositoryProvider {

    TraceRecorderRepository provide(TraceabilityDetails traceabilityDetails);
}
