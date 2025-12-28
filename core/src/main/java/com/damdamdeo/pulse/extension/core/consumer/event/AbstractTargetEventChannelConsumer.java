package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.LastConsumedAggregateVersion;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyKey;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyRepository;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.Topic;

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

    protected void handleMessage(final Target target, final FromApplication fromApplication,
                                 final EventKey eventKey, final EventValue eventValue) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventValue);
        LOGGER.fine("Consuming on target '%s' with key '%s' and value '%s'".formatted(target.name(), eventKey, eventValue));
        final AggregateRootType aggregateRootType = eventKey.toAggregateRootType();
        final AggregateId aggregateId = eventKey.toAggregateId();
        final CurrentVersionInConsumption currentVersionInConsumption = eventKey.toCurrentVersionInConsumption();
        final IdempotencyKey idempotencyKey = new IdempotencyKey(target, fromApplication, Topic.EVENT, aggregateRootType, aggregateId);
        final Optional<LastConsumedAggregateVersion> lastConsumedAggregateVersion = idempotencyRepository.findLastAggregateVersionBy(idempotencyKey);
        if (lastConsumedAggregateVersion.isPresent() && lastConsumedAggregateVersion.get().isBelow(currentVersionInConsumption)) {
            targetEventChannelExecutor.execute(target, fromApplication, eventKey, eventValue, lastConsumedAggregateVersion.get());
            idempotencyRepository.upsert(idempotencyKey, currentVersionInConsumption);
        } else if (lastConsumedAggregateVersion.isPresent()) {
            targetEventChannelExecutor.onAlreadyConsumed(target, fromApplication, eventKey, eventValue);
        } else {
            targetEventChannelExecutor.execute(target, fromApplication, eventKey, eventValue);
            idempotencyRepository.upsert(idempotencyKey, eventKey.toCurrentVersionInConsumption());
        }
    }
}
