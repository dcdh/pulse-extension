package com.damdamdeo.pulse.extension.writer.deployment.domainusecase;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.audience.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.query.UnableToResolveException;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.Set;

@ApplicationScoped
@Priority(1)
@Alternative
public class StubExecutedByResolver implements ExecutedByResolver {

    @Override
    public Set<ExecutedBy> resolve(final Set<AggregateId> aggregatesId) throws UnableToResolveException {
        throw new IllegalStateException("Should not be called");
    }

    @Override
    public Set<ExecutedBy> resolve(final OwnedBy ownedBy) throws UnableToResolveException {
        throw new IllegalStateException("Should not be called");
    }
}
