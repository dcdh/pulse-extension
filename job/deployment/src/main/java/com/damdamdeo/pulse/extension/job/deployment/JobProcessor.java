package com.damdamdeo.pulse.extension.job.deployment;

import com.damdamdeo.pulse.extension.core.job.SimpleScheduledJob;
import com.damdamdeo.pulse.extension.job.runtime.JdbcPostgresJobUnblockingLockManager;
import com.damdamdeo.pulse.extension.job.runtime.ScheduledJobRegistrar;
import com.damdamdeo.pulse.extension.job.runtime.batch.RegisterExecutedByJobName;
import com.damdamdeo.pulse.extension.job.runtime.executedby.ContextLocalsExecutionContextOverloader;
import com.damdamdeo.pulse.extension.job.runtime.executedby.JobExecutionContextProviderDecorator;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;

import java.util.List;

public class JobProcessor {

    private static final String FEATURE = "pulse-job-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerContextLocalsExecutionContextOverloader() {
        return AdditionalBeanBuildItem.builder().addBeanClass(ContextLocalsExecutionContextOverloader.class).build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerJdbcPostgresJobUnblockingLockManager() {
        return AdditionalBeanBuildItem.builder().addBeanClass(JdbcPostgresJobUnblockingLockManager.class).build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerScheduledJobRegistrar() {
        return AdditionalBeanBuildItem.builder().addBeanClass(ScheduledJobRegistrar.class).build();
    }

    @BuildStep
    void registerJobExecutionContextProviderDecorator(final BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItems,
                                                      final BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItems) {
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.builder().addBeanClass(JobExecutionContextProviderDecorator.class).build());
        additionalIndexedClassesBuildItems.produce(new AdditionalIndexedClassesBuildItem(JobExecutionContextProviderDecorator.class.getName()));
    }

    @BuildStep
    AdditionalBeanBuildItem registerRegisterExecutedByJobName(final CurateOutcomeBuildItem curateOutcomeBuildItem) {
        // io.quarkiverse.jberet:quarkus-jberet does not expose a Capability :/
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        final boolean isPresent = curateOutcomeBuildItem.getApplicationModel()
                .getDependencies().stream()
                .anyMatch(dep -> "io.quarkiverse.jberet".equals(dep.getGroupId())
                        && "quarkus-jberet".equals(dep.getArtifactId()));

        if (isPresent) {
            builder.addBeanClass(RegisterExecutedByJobName.class);
        }
        return builder.build();
    }

    @BuildStep
    List<AdditionalBeanBuildItem> registerScheduledJob(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex().getAllKnownSubclasses(SimpleScheduledJob.class)
                .stream()
                .map(scheduledJob -> AdditionalBeanBuildItem.builder()
                        .addBeanClass(scheduledJob.name().toString())
                        .setDefaultScope(DotNames.SINGLETON)
                        .setUnremovable()
                        .build())
                .toList();
    }
}
