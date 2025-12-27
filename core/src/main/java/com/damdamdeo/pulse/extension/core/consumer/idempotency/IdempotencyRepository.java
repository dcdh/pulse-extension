package com.damdamdeo.pulse.extension.core.consumer.idempotency;

import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.LastConsumedAggregateVersion;

import java.util.Optional;

public interface IdempotencyRepository {
    Optional<LastConsumedAggregateVersion> findLastAggregateVersionBy(IdempotencyKey idempotencyKey) throws IdempotencyException;

    void upsert(IdempotencyKey idempotencyKey, CurrentVersionInConsumption currentVersionInConsumption) throws IdempotencyException;
}
