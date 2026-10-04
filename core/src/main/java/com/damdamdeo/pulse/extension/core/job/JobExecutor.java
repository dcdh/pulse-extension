package com.damdamdeo.pulse.extension.core.job;

@FunctionalInterface
public interface JobExecutor {

    void execute() throws JobExecutionException;
}
