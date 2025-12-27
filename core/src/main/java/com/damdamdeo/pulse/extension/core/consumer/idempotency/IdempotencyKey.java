package com.damdamdeo.pulse.extension.core.consumer.idempotency;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Target;

import java.util.Objects;

// TODO introduce a discriminant via en enum
public record IdempotencyKey(Target target, FromApplication fromApplication, AggregateRootType aggregateRootType, AggregateId aggregateId) {

    public IdempotencyKey {
        Objects.requireNonNull(target);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(aggregateId);
    }
}
