package com.damdamdeo.pulse.extension.job.runtime.executedby;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.job.ExecutionContextOverloader;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import java.util.Set;

@Priority(1)
@Decorator
public class JobExecutionContextProviderDecorator implements ExecutionContextProvider {

    @Inject
    @Any
    @Delegate
    ExecutionContextProvider delegate;

    @Inject
    ExecutionContextOverloader executionContextOverloader;

    @Override
    public ExecutionContext provide() {
        return executionContextOverloader.current()
                .map(jobName -> new ExecutionContext(new ExecutedBy.Job(jobName), Set.of()))
                .orElseGet(() -> delegate.provide());
    }
}
