package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.Objects;

public record EncodedAggregateIdInvolved(Identifiable identifiable,
                                         Page<EncodedDetailedInvolved> page,
                                         Collection<EncodedNbOfExecutions> encodedNbOfExecutions) {

    public EncodedAggregateIdInvolved {
        Objects.requireNonNull(identifiable);
        Objects.requireNonNull(page);
        page.content().forEach(encodedDetailedInvolved ->
                Validate.validState(encodedDetailedInvolved.identifiable().id().equals(identifiable.id())));
        Objects.requireNonNull(encodedNbOfExecutions);
    }
}
