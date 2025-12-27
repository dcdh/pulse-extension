package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;

public interface EventKey {

    AggregateRootType toAggregateRootType();

    AggregateId toAggregateId();

    CurrentVersionInConsumption toCurrentVersionInConsumption();
}
