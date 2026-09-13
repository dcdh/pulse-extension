package com.damdamdeo.pulse.extension.traceability.deployment.finder;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.traceability.OwnedByProvider;
import com.damdamdeo.pulse.extension.core.traceability.OwnedByProviderException;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@ApplicationScoped
@Priority(1)
@Alternative
public class StubOwnedByProvider implements OwnedByProvider {

    @Override
    public OwnedBy provide(final AggregateId aggregateId) throws OwnedByProviderException {
        return Todo.OWNED_BY_USER_1;
    }
}
