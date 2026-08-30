package com.damdamdeo.pulse.extension.core.traceability;

import java.util.List;

public interface Traceable {

    List<AccessedAggregate> accessedAggregates();
}
