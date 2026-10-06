package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Set;

public sealed interface Result<A extends AggregateId, P extends Projection<A>>
        permits SingleResult, MultipleResult, MultiplePageableResult {

    Set<A> aggregateIds();
}
