package com.damdamdeo.pulse.extension.core.job;

public interface JobUnblockingLockManager {

    void executeWithUnblockingLock(JobName jobName, JobExecutor jobExecutor) throws JobLockingException, JobExecutionException;
}
