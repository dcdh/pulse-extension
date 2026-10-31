package com.damdamdeo.pulse.extension.job.deployment;

import com.damdamdeo.pulse.extension.core.job.SimpleScheduledJob;
import com.damdamdeo.pulse.extension.job.runtime.JdbcPostgresJobUnblockingLockManager;
import com.damdamdeo.pulse.extension.job.runtime.ScheduledJobRegistrar;
import com.damdamdeo.pulse.extension.job.runtime.batch.QueryToCommandProcessor;
import com.damdamdeo.pulse.extension.job.runtime.batch.QueryUseCaseReader;
import com.damdamdeo.pulse.extension.job.runtime.batch.RegisterExecutedByJobName;
import com.damdamdeo.pulse.extension.job.runtime.batch.UseCaseWriter;
import com.damdamdeo.pulse.extension.job.runtime.executedby.ContextLocalsExecutionContextOverloader;
import com.damdamdeo.pulse.extension.job.runtime.executedby.JobExecutionContextProviderDecorator;
import com.damdamdeo.pulse.extension.writer.deployment.UtilsProcessor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import jakarta.enterprise.context.Dependent;
import org.jboss.jandex.*;

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

    private boolean hasJberet(final CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return curateOutcomeBuildItem.getApplicationModel()
                .getDependencies().stream()
                .anyMatch(dep -> "io.quarkiverse.jberet".equals(dep.getGroupId())
                        && "quarkus-jberet".equals(dep.getArtifactId()));
    }

    @BuildStep
    AdditionalBeanBuildItem registerRegisterExecutedByJobName(final CurateOutcomeBuildItem curateOutcomeBuildItem) {
        // io.quarkiverse.jberet:quarkus-jberet does not expose a Capability :/
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        if (hasJberet(curateOutcomeBuildItem)) {
            builder.addBeanClass(RegisterExecutedByJobName.class);
        }
        return builder.build();
    }

    public static DotName DEPENDENT = DotName.createSimple(Dependent.class);

    @BuildStep
    List<AdditionalBeanBuildItem> registerQueryUseCaseReader(final CombinedIndexBuildItem combinedIndexBuildItem,
                                                             final CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (hasJberet(curateOutcomeBuildItem)) {
            return combinedIndexBuildItem.getIndex().getAllKnownSubclasses(QueryUseCaseReader.class)
                    .stream()
                    .map(useCase -> AdditionalBeanBuildItem.builder()
                            .addBeanClass(useCase.name().toString())
                            .setDefaultScope(DEPENDENT)
                            .setUnremovable()
                            .build())
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalBeanBuildItem> registerQueryToCommandProcessor(final CombinedIndexBuildItem combinedIndexBuildItem,
                                                                  final CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (hasJberet(curateOutcomeBuildItem)) {
            return combinedIndexBuildItem.getIndex().getAllKnownSubclasses(QueryToCommandProcessor.class)
                    .stream()
                    .map(useCase -> AdditionalBeanBuildItem.builder()
                            .addBeanClass(useCase.name().toString())
                            .setDefaultScope(DEPENDENT)
                            .setUnremovable()
                            .build())
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalBeanBuildItem> registerUseCaseWriter(final CombinedIndexBuildItem combinedIndexBuildItem,
                                                        final CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (hasJberet(curateOutcomeBuildItem)) {
            return combinedIndexBuildItem.getIndex().getAllKnownSubclasses(UseCaseWriter.class)
                    .stream()
                    .map(useCase -> AdditionalBeanBuildItem.builder()
                            .addBeanClass(useCase.name().toString())
                            .setDefaultScope(DEPENDENT)
                            .setUnremovable()
                            .build())
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    AnnotationsTransformerBuildItem addNamedAnnotationToChunkProcessors(final CombinedIndexBuildItem combinedIndexBuildItem,
                                                                        final CurateOutcomeBuildItem curateOutcomeBuildItem) {
        final IndexView index = combinedIndexBuildItem.getIndex();
        final AnnotationInstance namedAnnotation = AnnotationInstance.create(DotNames.NAMED, null,
                new AnnotationValue[0]);

        if (hasJberet(curateOutcomeBuildItem)) {
            return new AnnotationsTransformerBuildItem(
                    (AnnotationTransformation) context -> {
                        final Declaration target = context.declaration();
                        if (target.kind() == AnnotationTarget.Kind.CLASS) {
                            if (UtilsProcessor.hasSuperClass(target.asClass(), index, QueryUseCaseReader.class)
                                    || UtilsProcessor.hasSuperClass(target.asClass(), index, QueryToCommandProcessor.class)
                                    || UtilsProcessor.hasSuperClass(target.asClass(), index, UseCaseWriter.class)) {
                                context.add(namedAnnotation);
                            }
                        }
                    }
            );
        } else {
            return null;
        }
    }
}
