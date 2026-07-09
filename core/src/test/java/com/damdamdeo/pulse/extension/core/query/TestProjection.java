package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.UserId;

import java.util.List;

public record TestProjection(List<AggregateId> aggregateIds) implements Projection {

    public static TestProjection PROJECTION_USER_1 = new TestProjection(List.of(UserId.USER_1));
}
