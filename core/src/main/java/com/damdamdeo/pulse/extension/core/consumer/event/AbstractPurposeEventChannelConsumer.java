package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyKey;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public abstract class AbstractPurposeEventChannelConsumer<T> {

    final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private final PurposeEventChannelExecutor<T> purposeEventChannelExecutor;
    private final IdempotencyRepository idempotencyRepository;

    public AbstractPurposeEventChannelConsumer(final PurposeEventChannelExecutor<T> purposeEventChannelExecutor,
                                               final IdempotencyRepository idempotencyRepository) {
        this.purposeEventChannelExecutor = Objects.requireNonNull(purposeEventChannelExecutor);
        this.idempotencyRepository = Objects.requireNonNull(idempotencyRepository);
    }

    protected void handleMessage(final Purpose purpose, final FromApplication fromApplication,
                                 final EventKey eventKey, final EventValue eventValue) {
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(eventKey);
        Objects.requireNonNull(eventValue);
        LOGGER.fine("Consuming on target '%s' with key '%s' and value '%s'".formatted(purpose.name(), eventKey, eventValue));
        final AggregateRootType aggregateRootType = eventKey.toAggregateRootType();
        final AggregateId aggregateId = eventKey.toAggregateId();
        final CurrentVersionInConsumption currentVersionInConsumption = eventKey.toCurrentVersionInConsumption();
        final IdempotencyKey idempotencyKey = new IdempotencyKey(purpose, fromApplication, Table.EVENT, aggregateRootType, aggregateId);
        final Optional<LastConsumedAggregateVersion> lastConsumedAggregateVersion = idempotencyRepository.findLastAggregateVersionBy(idempotencyKey);
        if (lastConsumedAggregateVersion.isPresent() && lastConsumedAggregateVersion.get().isBelow(currentVersionInConsumption)) {
            purposeEventChannelExecutor.execute(purpose, fromApplication, eventKey, eventValue, lastConsumedAggregateVersion.get());
            idempotencyRepository.upsert(idempotencyKey, currentVersionInConsumption);
        } else if (lastConsumedAggregateVersion.isPresent()) {
            purposeEventChannelExecutor.onAlreadyConsumed(purpose, fromApplication, eventKey, eventValue);
        } else {
            purposeEventChannelExecutor.execute(purpose, fromApplication, eventKey, eventValue);
            idempotencyRepository.upsert(idempotencyKey, eventKey.toCurrentVersionInConsumption());
        }
    }
}
