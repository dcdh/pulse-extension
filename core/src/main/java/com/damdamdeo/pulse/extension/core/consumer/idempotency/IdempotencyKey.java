package com.damdamdeo.pulse.extension.core.consumer.idempotency;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;

import java.util.Objects;

public record IdempotencyKey(Purpose purpose, FromApplication fromApplication, Topic topic, AggregateRootType aggregateRootType, AggregateId aggregateId) {

    public IdempotencyKey {
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(topic);
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(aggregateId);
    }
}
