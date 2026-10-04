package com.damdamdeo.pulse.extension.job.runtime.executedby;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.job.ExecutionContextOverloader;
import com.damdamdeo.pulse.extension.core.job.JobName;
import io.quarkus.arc.Unremovable;
import io.smallrye.common.vertx.ContextLocals;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
@Unremovable
public class ContextLocalsExecutionContextOverloader implements ExecutionContextOverloader {

    @Override
    public void overload(final JobName jobName) {
        Objects.requireNonNull(jobName);
        ContextLocals.put(ExecutedBy.Job.DISCRIMINANT, jobName);
    }

    @Override
    public void clean(final JobName jobName) {
        Objects.requireNonNull(jobName);
        ContextLocals.remove(ExecutedBy.Job.DISCRIMINANT);
    }

    @Override
    public Optional<JobName> current() {
        return ContextLocals.get(ExecutedBy.Job.DISCRIMINANT);
    }
}
