package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public abstract class AbstractTargetEventChannelConsumer<T> {

    final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private final TargetEventChannelExecutor<T> targetEventChannelExecutor;
    private final IdempotencyRepository idempotencyRepository;

    public AbstractTargetEventChannelConsumer(final TargetEventChannelExecutor<T> targetEventChannelExecutor,
                                              final IdempotencyRepository idempotencyRepository) {
        this.targetEventChannelExecutor = Objects.requireNonNull(targetEventChannelExecutor);
        this.idempotencyRepository = Objects.requireNonNull(idempotencyRepository);
    }

    protected void handleMessage(final Target target, final ApplicationNaming source, final EventKey eventKey, final EventValue eventValue) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(source);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventValue);
        LOGGER.fine("Consuming on target '%s' with key '%s' and value '%s'".formatted(target.name(), eventKey, eventValue));
        final AggregateRootType aggregateRootType = eventKey.toAggregateRootType();
        final AggregateId aggregateId = eventKey.toAggregateId();
        final CurrentVersionInConsumption currentVersionInConsumption = eventKey.toCurrentVersionInConsumption();

        final Optional<LastConsumedAggregateVersion> lastConsumedAggregateVersion = idempotencyRepository.findLastAggregateVersionBy(
                target, source, aggregateRootType, aggregateId);
        if (lastConsumedAggregateVersion.isPresent() && lastConsumedAggregateVersion.get().isBelow(currentVersionInConsumption)) {
            targetEventChannelExecutor.execute(target, source, eventKey, eventValue, lastConsumedAggregateVersion.get());
            idempotencyRepository.upsert(target, source, eventKey);
        } else if (lastConsumedAggregateVersion.isPresent()) {
            targetEventChannelExecutor.onAlreadyConsumed(target, source, eventKey, eventValue);
        } else {
            targetEventChannelExecutor.execute(target, source, eventKey, eventValue);
            idempotencyRepository.upsert(target, source, eventKey);
        }
    }
}
