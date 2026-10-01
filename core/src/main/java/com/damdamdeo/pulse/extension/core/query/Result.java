package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.traceability.Traceable;

public sealed interface Result<A extends AggregateId, P extends Projection<A>> extends Traceable<A>
        permits SingleResult, MultipleResult, MultiplePageableResult {
}
