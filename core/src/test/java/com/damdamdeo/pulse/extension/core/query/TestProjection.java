package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.UserId;

public record TestProjection(UserId userId) implements Projection<UserId> {

    @Override
    public ProjectionId<UserId> id() {
        return ProjectionId.from(userId);
    }
}
