package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.job.JobName;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.Objects;
import java.util.Set;

public record ExecutedBySpecificJob<K extends AggregateId>(JobName jobName) implements Permission<K> {

    public ExecutedBySpecificJob {
        Objects.requireNonNull(jobName);
    }

    @Override
    public boolean allow(final Set<K> aggregateIds, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(aggregateIds);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        return executionContext.executedBy().equals(new ExecutedBy.Job(jobName));
    }

    @Override
    public int priority() {
        return 8;
    }
}
