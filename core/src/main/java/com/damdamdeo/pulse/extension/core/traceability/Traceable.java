package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Set;

public interface Traceable<A extends AggregateId> {

    default Set<A> aggregateIds() {
        // do not log when empty
        return Set.of();
    }
}
