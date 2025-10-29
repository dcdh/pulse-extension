package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;

public interface AggregateRootLoader<T> {

    AggregateRootLoaded<T> getByAggregateRootTypeAndAggregateId(final AggregateRootType aggregateRootType, final AggregateId aggregateId)
            throws UnknownAggregateRootException, AggregateRootLoaderException;
}
