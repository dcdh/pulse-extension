package com.damdamdeo.pulse.extension.job.runtime.batch;

import com.damdamdeo.pulse.extension.core.job.ExecutionContextOverloader;
import com.damdamdeo.pulse.extension.core.job.JobName;
import jakarta.batch.api.listener.JobListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@Dependent
public class RegisterExecutedByJobName implements JobListener {

    @Inject
    ExecutionContextOverloader executionContextOverloader;

    @Inject
    JobContext jobContext;

    @Override
    public void beforeJob() throws Exception {
        executionContextOverloader.overload(new JobName(jobContext.getJobName()));
    }

    @Override
    public void afterJob() throws Exception {
        executionContextOverloader.clean(new JobName(jobContext.getJobName()));
    }
}
