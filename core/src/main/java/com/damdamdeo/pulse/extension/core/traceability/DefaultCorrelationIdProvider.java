package com.damdamdeo.pulse.extension.core.traceability;

import java.util.Optional;

public final class DefaultCorrelationIdProvider implements CorrelationIdProvider {

    private final CorrelationIdGenerator correlationIdGenerator;
    private final CorrelationIdProviderContextualStorage correlationIdProviderContextualStorage;

    public DefaultCorrelationIdProvider(final CorrelationIdGenerator correlationIdGenerator,
                                        final CorrelationIdProviderContextualStorage correlationIdProviderContextualStorage) {
        this.correlationIdGenerator = correlationIdGenerator;
        this.correlationIdProviderContextualStorage = correlationIdProviderContextualStorage;
    }

    @Override
    public CorrelationId provide() throws CorrelationIdProviderException {
        final Optional<CorrelationId> retrieved = correlationIdProviderContextualStorage.retrieve();
        if (retrieved.isPresent()) {
            return retrieved.get();
        } else {
            try {
                final CorrelationId correlationId = correlationIdGenerator.generate();
                return correlationIdProviderContextualStorage.store(correlationId);
            } catch (final CorrelationIdGeneratorException exception) {
                throw new CorrelationIdProviderException(exception);
            }
        }
    }
}
