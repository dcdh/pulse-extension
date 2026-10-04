package com.damdamdeo.pulse.extension.job.runtime;

import com.damdamdeo.pulse.extension.core.job.JobExecutionException;
import com.damdamdeo.pulse.extension.core.job.ScheduledJob;
import io.quarkus.arc.All;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ApplicationScoped
@Unremovable
public class ScheduledJobRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledJobRegistrar.class);

    @Inject
    @All
    List<ScheduledJob<?, ?, ?, ?, ?, ?>> scheduledJobs;

    @Inject
    Scheduler scheduler;

    void onStart(@Observes final StartupEvent startupEvent) {
        LOGGER.info("Registering {} scheduled jobs", scheduledJobs.size());
        scheduledJobs.forEach(scheduledJob ->
                scheduler.newJob(scheduledJob.jobName().name())
                        .setCron(scheduledJob.cron().cron())
                        .setConcurrentExecution(Scheduled.ConcurrentExecution.SKIP)
                        .setTask(task -> {
                            try {
                                scheduledJob.execute();
                            } catch (final JobExecutionException exception) {
                                throw new RuntimeException(exception);
                            }
                        })
                        .schedule());
    }
}
