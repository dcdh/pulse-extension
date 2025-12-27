package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.CurrentVersionInConsumption;

public interface EventKey {

    AggregateRootType toAggregateRootType();

    AggregateId toAggregateId();

    CurrentVersionInConsumption toCurrentVersionInConsumption();
}
