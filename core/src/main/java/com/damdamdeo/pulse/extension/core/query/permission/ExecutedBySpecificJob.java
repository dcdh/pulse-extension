package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.job.JobName;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.Objects;

public record ExecutedBySpecificJob<K extends AggregateId>(JobName jobName) implements CompositePermission<K> {

    public ExecutedBySpecificJob {
        Objects.requireNonNull(jobName);
    }

    @Override
    public boolean allow(final ProjectionId<K> projectionId, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(projectionId);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        return executionContext.executedBy().equals(new ExecutedBy.Job(jobName));
    }
}
