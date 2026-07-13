package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.UserId;

public record TestProjection(UserId userId) implements Projection {

    public static TestProjection PROJECTION_USER_1 = new TestProjection(UserId.USER_1);

    @Override
    public AggregateId id() {
        return userId;
    }
}
