package com.damdamdeo.pulse.extension.core.consumer.idempotency;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;
import com.damdamdeo.pulse.extension.core.consumer.Table;

import java.util.Objects;

public record IdempotencyKey(Purpose purpose, FromApplication fromApplication, Table table, AggregateRootType aggregateRootType, AggregateId aggregateId) {

    public IdempotencyKey {
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(table);
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(aggregateId);
    }
}
