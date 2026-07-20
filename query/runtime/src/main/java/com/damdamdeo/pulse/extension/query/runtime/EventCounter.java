package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface EventCounter {

    Integer byOwnedBy(OwnedBy ownedBy) throws EventCounterException;

    Integer byAggregateId(AggregateId aggregateId) throws EventCounterException;
}
