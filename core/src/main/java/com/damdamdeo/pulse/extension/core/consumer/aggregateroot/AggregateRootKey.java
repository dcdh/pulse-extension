package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;

public interface AggregateRootKey {

    AggregateRootType toAggregateRootType();

    AggregateId toAggregateId();

    CurrentVersionInConsumption toCurrentVersionInConsumption();
}
