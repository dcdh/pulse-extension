package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.query.permission.Permission;
import com.damdamdeo.pulse.extension.core.traceability.TraceabilityDetails;

import java.util.List;

public interface QueryUseCase<A extends Identifiable, I extends Input, P extends Projection<A>, R extends Result<A, P>> {

    List<Permission<A>> permissions();

    default TraceabilityDetails traceabilityDetails() {
        return TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY;
    }
}
