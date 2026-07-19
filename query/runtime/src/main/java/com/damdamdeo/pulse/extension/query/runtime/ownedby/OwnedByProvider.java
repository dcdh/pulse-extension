package com.damdamdeo.pulse.extension.query.runtime.ownedby;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface OwnedByProvider {

    OwnedBy getByAggregateId(AggregateId aggregateId) throws UnableToProvideOwnedByException;
}
