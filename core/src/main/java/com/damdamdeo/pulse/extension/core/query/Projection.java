package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;

public interface Projection<A extends AggregateId> {

    A id();
}
