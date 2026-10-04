package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.traceability.CorrelationId;
import com.damdamdeo.pulse.extension.core.traceability.CorrelationIdProviderContextualStorage;
import io.quarkus.arc.Unremovable;
import io.smallrye.common.vertx.ContextLocals;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
@Unremovable
public class ContextLocalsCorrelationIdProviderContextualStorage implements CorrelationIdProviderContextualStorage {

    public static final String CORRELATION_ID = "CorrelationId";

    @Override
    public CorrelationId store(final CorrelationId correlationId) {
        Objects.requireNonNull(correlationId);
        ContextLocals.put(CORRELATION_ID, correlationId);
        return correlationId;
    }

    @Override
    public Optional<CorrelationId> retrieve() {
        return ContextLocals.get(CORRELATION_ID);
    }
}
