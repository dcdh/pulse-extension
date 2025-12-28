package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

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

public abstract class AbstractTargetAggregateRootChannelConsumer<T> {

    final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private final TargetAggregateRootChannelExecutor<T> targetAggregateRootChannelExecutor;
    private final IdempotencyRepository idempotencyRepository;

    public AbstractTargetAggregateRootChannelConsumer(final TargetAggregateRootChannelExecutor<T> targetAggregateRootChannelExecutor,
                                                      final IdempotencyRepository idempotencyRepository) {
        this.targetAggregateRootChannelExecutor = Objects.requireNonNull(targetAggregateRootChannelExecutor);
        this.idempotencyRepository = Objects.requireNonNull(idempotencyRepository);
    }

    protected void handleMessage(final Target target, final FromApplication fromApplication,
                                 final AggregateRootKey aggregateRootKey, final AggregateRootValue aggregateRootValue) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(aggregateRootKey);
        Objects.requireNonNull(aggregateRootValue);
        LOGGER.fine("Consuming on target '%s' with key '%s' and value '%s'".formatted(target.name(), aggregateRootKey, aggregateRootValue));
        final AggregateRootType aggregateRootType = aggregateRootKey.toAggregateRootType();
        final AggregateId aggregateId = aggregateRootKey.toAggregateId();
        final CurrentVersionInConsumption currentVersionInConsumption = aggregateRootKey.toCurrentVersionInConsumption();
        final IdempotencyKey idempotencyKey = new IdempotencyKey(target, fromApplication, Topic.AGGREGATE_ROOT, aggregateRootType, aggregateId);
        final Optional<LastConsumedAggregateVersion> lastConsumedAggregateVersion = idempotencyRepository.findLastAggregateVersionBy(idempotencyKey);
        if (lastConsumedAggregateVersion.isPresent() && lastConsumedAggregateVersion.get().isBelow(currentVersionInConsumption)) {
            targetAggregateRootChannelExecutor.execute(target, fromApplication, aggregateRootKey, aggregateRootValue, lastConsumedAggregateVersion.get());
            idempotencyRepository.upsert(idempotencyKey, aggregateRootKey.toCurrentVersionInConsumption());
        } else if (lastConsumedAggregateVersion.isPresent()) {
            targetAggregateRootChannelExecutor.onAlreadyConsumed(target, fromApplication, aggregateRootKey, aggregateRootValue);
        } else {
            targetAggregateRootChannelExecutor.execute(target, fromApplication, aggregateRootKey, aggregateRootValue);
            idempotencyRepository.upsert(idempotencyKey, aggregateRootKey.toCurrentVersionInConsumption());
        }
    }
}
