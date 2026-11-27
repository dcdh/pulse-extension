package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.traceability.runtime.*;
import com.damdamdeo.pulse.extension.traceability.runtime.api.FinderExceptionMapper;
import com.damdamdeo.pulse.extension.traceability.runtime.api.TraceabilityInvolvedFinderEndpoint;
import com.damdamdeo.pulse.extension.traceability.runtime.api.TraceabilityParamConverterProvider;
import com.damdamdeo.pulse.extension.traceability.runtime.api.serialization.TraceabilityObjectMapperProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

import java.util.List;
import java.util.function.BooleanSupplier;

public class BeansProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerTraceIdGenerator(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        switch (traceabilityConfiguration.tracingMode()) {
            case DISABLED -> builder.addBeanClass(NoOpTraceIdGenerator.class)
                    .setUnremovable()
                    .setDefaultScope(DotNames.APPLICATION_SCOPED);
            case INVOLVED, INVOLVED_WITH_FULL_DETAILS -> builder.addBeanClass(JdbcPostgresTraceIdGenerator.class);
        }
        return builder.build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerCorrelationIdProvider(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        switch (traceabilityConfiguration.tracingMode()) {
            case DISABLED -> builder.addBeanClass(NoOpCorrelationIdProvider.class)
                    .setUnremovable()
                    .setDefaultScope(DotNames.APPLICATION_SCOPED);
            case INVOLVED, INVOLVED_WITH_FULL_DETAILS -> builder.addBeanClass(DefaultCorrelationIdProvider.class)
                    .setUnremovable()
                    .setDefaultScope(DotNames.APPLICATION_SCOPED)
                    .addBeanClass(JdbcPostgresCorrelationIdGenerator.class)
                    .addBeanClass(ContextLocalsCorrelationIdProviderContextualStorage.class);
        }
        return builder.build();
    }

    @BuildStep(onlyIf = IsTraceabilityEnabled.class)
    AdditionalBeanBuildItem registerTraceRecorderRepository() {
        return AdditionalBeanBuildItem.builder().addBeanClass(JdbcPostgresTraceRecorderRepository.class)
                .build();
    }

    @BuildStep(onlyIf = IsTraceabilityEnabled.class)
    AdditionalBeanBuildItem registerOwnedByProvider() {
        return AdditionalBeanBuildItem.builder().addBeanClass(JdbcPostgresOwnedByProvider.class)
                .build();
    }

    @BuildStep(onlyIf = IsTraceabilityEnabled.class)
    AdditionalBeanBuildItem registerExecutedByEncodedProvider() {
        return AdditionalBeanBuildItem.builder().addBeanClass(ExecutedByEncodedProvider.class)
                .build();
    }

    @BuildStep(onlyIf = IsTraceabilityEnabled.class)
    AdditionalBeanBuildItem registerJdbcPostgresExecutedByEncodedRepository() {
        return AdditionalBeanBuildItem.builder().addBeanClass(JdbcPostgresExecutedByEncodedRepository.class)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerTraceAppender(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        switch (traceabilityConfiguration.tracingMode()) {
            case DISABLED -> builder.addBeanClass(NoOpTraceAppender.class);
            case INVOLVED, INVOLVED_WITH_FULL_DETAILS -> builder.addBeanClass(DefaultTraceAppender.class);
        }
        return builder.build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerExecutedAtProvider(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        switch (traceabilityConfiguration.tracingMode()) {
            case DISABLED -> builder.addBeanClass(NoOpExecutedAtProvider.class);
            case INVOLVED, INVOLVED_WITH_FULL_DETAILS -> builder.addBeanClass(DefaultExecutedAtProvider.class);
        }
        return builder.build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerDetailedInvolvedFinder(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        switch (traceabilityConfiguration.tracingMode()) {
            case DISABLED -> builder.addBeanClass(NoOpInvolvedFinder.class)
                    .setUnremovable()
                    .setDefaultScope(DotNames.APPLICATION_SCOPED);
            case INVOLVED, INVOLVED_WITH_FULL_DETAILS -> builder.addBeanClass(DefaultInvolvedFinder.class)
                    .setUnremovable()
                    .setDefaultScope(DotNames.APPLICATION_SCOPED);
        }
        return builder.build();
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem registerFileParamConverterProvider() {
        return new AdditionalIndexedClassesBuildItem(TraceabilityParamConverterProvider.class.getName());
    }

    @BuildStep(onlyIf = IsTraceabilityEnabled.class)
    AdditionalBeanBuildItem registerTraceabilityObjectMapperProducer() {
        return AdditionalBeanBuildItem.builder().addBeanClass(TraceabilityObjectMapperProducer.class)
                .build();
    }

    @BuildStep(onlyIf = IsTraceabilityEnabled.class)
    void registerTraceabilityFinderDetailedInvolvedEndpoint(final BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItemBuildProducer,
                                                            final BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalIndexedClassesBuildItemBuildProducer.produce(new AdditionalIndexedClassesBuildItem(
                TraceabilityInvolvedFinderEndpoint.class.getName()));
        additionalBeanBuildItemBuildProducer.produce(AdditionalBeanBuildItem.builder().addBeanClasses(
                TraceabilityInvolvedFinderEndpoint.class).build());
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem registerFinderExceptionMapper() {
        return new AdditionalIndexedClassesBuildItem(FinderExceptionMapper.class.getName());
    }

    @BuildStep(onlyIf = IsTraceabilityEnabled.class)
    AdditionalBeanBuildItem registerEncodedInvolvedRepository() {
        return AdditionalBeanBuildItem.builder().addBeanClass(JdbcPostgresInvolvedFinderRepository.class)
                .build();
    }

    @BuildStep
    List<AdditionalBeanBuildItem> registerTraceabilityFeature() {
        return List.of(
                AdditionalBeanBuildItem.builder().addBeanClass(TraceabilityFeature.class)
                        .setUnremovable()
                        .setDefaultScope(DotNames.APPLICATION_SCOPED).build(),
                AdditionalBeanBuildItem.builder().addBeanClass(SmallryeTraceabilityRoleProvider.class)
                        .build());
    }

    static final class IsTraceabilityEnabled implements BooleanSupplier {

        private final boolean enabled;

        IsTraceabilityEnabled(final TraceabilityConfiguration config) {
            this.enabled = TracingMode.INVOLVED.equals(config.tracingMode())
                    || TracingMode.INVOLVED_WITH_FULL_DETAILS.equals(config.tracingMode());
        }

        @Override
        public boolean getAsBoolean() {
            return enabled;
        }
    }
}
