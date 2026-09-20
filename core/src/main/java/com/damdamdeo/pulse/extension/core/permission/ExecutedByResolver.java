package com.damdamdeo.pulse.extension.core.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.UnableToResolveException;

import java.util.Set;

public interface ExecutedByResolver {

    Set<ExecutedBy> resolve(Set<AggregateId> aggregatesId) throws UnableToResolveException;

    Set<ExecutedBy> resolve(OwnedBy ownedBy) throws UnableToResolveException;
}
