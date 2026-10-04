package com.damdamdeo.pulse.extension.core.traceability;

import java.util.Optional;

public interface CorrelationIdProviderContextualStorage {

    CorrelationId store(CorrelationId correlationId);

    Optional<CorrelationId> retrieve();
}
