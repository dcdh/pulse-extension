package com.damdamdeo.pulse.extension.job.runtime.executedby;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.job.ExecutionContextOverloader;
import com.damdamdeo.pulse.extension.core.job.JobName;
import io.quarkus.arc.Unremovable;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
@Unremovable
public class ContextLocalsExecutionContextOverloader implements ExecutionContextOverloader {

    // Blocking batch executors do not necessarily have a duplicated Vert.x context.
    private final ThreadLocal<JobName> blockingJob = new ThreadLocal<>();

    @Override
    public void overload(final JobName jobName) {
        Objects.requireNonNull(jobName);
        if (VertxContext.isOnDuplicatedContext()) {
            ContextLocals.put(ExecutedBy.Job.DISCRIMINANT, jobName);
        } else {
            blockingJob.set(jobName);
        }
    }

    @Override
    public void clean(final JobName jobName) {
        Objects.requireNonNull(jobName);
        if (VertxContext.isOnDuplicatedContext()) {
            ContextLocals.remove(ExecutedBy.Job.DISCRIMINANT);
        } else {
            blockingJob.remove();
        }
    }

    @Override
    public Optional<JobName> current() {
        return VertxContext.isOnDuplicatedContext()
                ? ContextLocals.get(ExecutedBy.Job.DISCRIMINANT)
                : Optional.ofNullable(blockingJob.get());
    }
}
