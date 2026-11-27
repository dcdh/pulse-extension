package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.pagination.Page;

import java.util.List;
import java.util.Objects;

public record AggregateIdInvolved(Identifiable identifiable,
                                  AnyAggregateId linkTo,
                                  Page<DetailedInvolved> page,
                                  List<NbOfExecutions> nbOfExecutions) {

    public AggregateIdInvolved {
        Objects.requireNonNull(identifiable);
        Objects.requireNonNull(linkTo);
        Objects.requireNonNull(page);
        Objects.requireNonNull(nbOfExecutions);
    }
}
