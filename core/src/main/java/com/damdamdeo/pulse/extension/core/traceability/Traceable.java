package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;

import java.util.Set;

public interface Traceable {

    default Set<AnyAggregateId> anyAggregateIds() {
        // do not log when empty
        return Set.of();
    }
}
