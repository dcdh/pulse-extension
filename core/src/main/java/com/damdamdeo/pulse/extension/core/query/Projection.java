package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

public interface Projection<A extends Identifiable> {

    ProjectionId<A> id();
}
