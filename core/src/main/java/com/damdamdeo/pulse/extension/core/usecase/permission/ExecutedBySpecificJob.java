package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.job.JobName;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.Objects;

public record ExecutedBySpecificJob<K extends AggregateId, C extends Command<K>>(
        JobName jobName) implements CompositePermission<K, C> {

    public ExecutedBySpecificJob {
        Objects.requireNonNull(jobName);
    }

    @Override
    public boolean allow(final K aggregateId, final C command, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(command);
        Objects.requireNonNull(permissionExecutionContext);
        return allow(command, permissionExecutionContext);
    }

    @Override
    public boolean allow(final C command, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        return executionContext.executedBy().equals(new ExecutedBy.Job(jobName));
    }
}
