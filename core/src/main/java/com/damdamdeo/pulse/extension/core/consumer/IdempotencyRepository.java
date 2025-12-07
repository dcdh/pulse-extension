package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;

import java.util.Optional;

public interface IdempotencyRepository {
    Optional<LastConsumedAggregateVersion> findLastAggregateVersionBy(Target target, FromApplication fromApplication, AggregateRootType aggregateRootType, AggregateId aggregateId)
            throws IdempotencyException;

    void upsert(Target target, FromApplication fromApplication, EventKey eventKey) throws IdempotencyException;
}
