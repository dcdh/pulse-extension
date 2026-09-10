package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface OwnedByProvider {

    OwnedBy provide(AnyAggregateId aggregateId) throws OwnedByProviderException;
}
