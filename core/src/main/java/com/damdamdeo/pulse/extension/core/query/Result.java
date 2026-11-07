package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.List;
import java.util.Set;

public sealed interface Result<A extends Identifiable, P extends Projection<A>>
        permits SingleResult, MultipleResult, MultiplePageableResult {

    Set<ProjectionId<A>> projectionIds();

    List<P> projections();
}
