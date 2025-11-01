package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import org.apache.commons.lang3.Validate;

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

    protected void handleMessage(final Target target, final ApplicationNaming source, final EventKey eventKey, final EventRecord eventRecord) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(source);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventRecord);
        LOGGER.fine("Consuming on target '%s' with key '%s' and value '%s'".formatted(target.name(), eventKey, eventRecord));
        Validate.validState(eventRecord.match(eventKey), "Key mismatch with payload !");
        final AggregateRootType aggregateRootType = eventRecord.toAggregateRootType();
        final AggregateId aggregateId = eventRecord.toAggregateId();
        final CurrentVersionInConsumption currentVersionInConsumption = eventRecord.toCurrentVersionInConsumption();

        final Optional<LastConsumedAggregateVersion> lastConsumedAggregateVersion = idempotencyRepository.findLastAggregateVersionBy(
                target, source, aggregateRootType, aggregateId);
        if (lastConsumedAggregateVersion.isPresent() && lastConsumedAggregateVersion.get().isBelow(currentVersionInConsumption)) {
            targetEventChannelExecutor.execute(target, source, eventKey, eventRecord, lastConsumedAggregateVersion.get());
            idempotencyRepository.upsert(target, source, eventKey);
        } else if (lastConsumedAggregateVersion.isPresent()) {
            targetEventChannelExecutor.onAlreadyConsumed(target, source, eventKey, eventRecord);
        } else {
            targetEventChannelExecutor.execute(target, source, eventKey, eventRecord);
            idempotencyRepository.upsert(target, source, eventKey);
        }
    }
}
