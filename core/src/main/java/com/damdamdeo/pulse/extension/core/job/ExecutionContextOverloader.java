package com.damdamdeo.pulse.extension.core.job;

import java.util.Optional;

public interface ExecutionContextOverloader {

    void overload(JobName jobName);

    void clean(JobName jobName);

    Optional<JobName> current();
}
