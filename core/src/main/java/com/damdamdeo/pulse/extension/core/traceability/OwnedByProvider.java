package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface OwnedByProvider {

    OwnedBy provide(AggregateId aggregateId) throws OwnedByProviderException;
}
