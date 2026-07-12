package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.Set;

public interface ExecutedByResolver {

    Set<ExecutedBy> resolve(Set<AggregateId> aggregatesId);
}
