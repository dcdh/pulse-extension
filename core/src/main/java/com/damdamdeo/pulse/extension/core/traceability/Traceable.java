package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Set;

public interface Traceable {

    default Set<AggregateId> aggregateIds() {
        // do not log when empty
        return Set.of();
    }
}
