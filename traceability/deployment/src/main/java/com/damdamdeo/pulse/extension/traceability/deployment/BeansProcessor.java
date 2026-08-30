package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.traceability.runtime.*;
import com.damdamdeo.pulse.extension.traceability.runtime.api.FinderExceptionMapper;
import com.damdamdeo.pulse.extension.traceability.runtime.api.TraceabilityFinderDetailedInvolvedEndpoint;
import com.damdamdeo.pulse.extension.traceability.runtime.api.TraceabilityFinderInvolvedEndpoint;
import com.damdamdeo.pulse.extension.traceability.runtime.api.TraceabilityParamConverterProvider;
import com.damdamdeo.pulse.extension.traceability.runtime.api.deserializer.TraceabilityObjectMapperProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

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
    AdditionalBeanBuildItem registerTraceRecorderRepository(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        switch (traceabilityConfiguration.tracingMode()) {
            case DISABLED -> {
            }
            case INVOLVED -> builder.addBeanClass(JdbcPostgresInvolvedTraceRecorderRepository.class);
            case INVOLVED_WITH_FULL_DETAILS ->
                    builder.addBeanClass(JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository.class);
        }
        return builder.build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerOwnedByProvider(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        if (traceabilityConfiguration.tracingMode().equals(TracingMode.INVOLVED)
                || traceabilityConfiguration.tracingMode().equals(TracingMode.INVOLVED_WITH_FULL_DETAILS)) {
            builder.addBeanClass(JdbcPostgresOwnedByProvider.class);
        }
        return builder.build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerExecutedByEncodedProvider(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        if (traceabilityConfiguration.tracingMode().equals(TracingMode.INVOLVED)
                || traceabilityConfiguration.tracingMode().equals(TracingMode.INVOLVED_WITH_FULL_DETAILS)) {
            builder.addBeanClass(ExecutedByEncodedProvider.class);
        }
        return builder.build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerJdbcPostgresExecutedByEncodedRepository(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        if (traceabilityConfiguration.tracingMode().equals(TracingMode.INVOLVED)
                || traceabilityConfiguration.tracingMode().equals(TracingMode.INVOLVED_WITH_FULL_DETAILS)) {
            builder.addBeanClass(JdbcPostgresExecutedByEncodedRepository.class);
        }
        return builder.build();
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
    AdditionalBeanBuildItem registerInvolvedFinder(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        switch (traceabilityConfiguration.tracingMode()) {
            case DISABLED -> builder.addBeanClass(NoOpInvolvedFinder.class);
            case INVOLVED, INVOLVED_WITH_FULL_DETAILS -> builder.addBeanClass(DefaultInvolvedFinder.class);
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
            case INVOLVED, DISABLED -> builder.addBeanClass(NoOpDetailedInvolvedFinder.class)
                    .setUnremovable()
                    .setDefaultScope(DotNames.APPLICATION_SCOPED);
            case INVOLVED_WITH_FULL_DETAILS -> builder.addBeanClass(DefaultDetailedInvolvedFinder.class)
                    .setUnremovable()
                    .setDefaultScope(DotNames.APPLICATION_SCOPED);
        }
        return builder.build();
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem registerFileParamConverterProvider() {
        return new AdditionalIndexedClassesBuildItem(TraceabilityParamConverterProvider.class.getName());
    }

    @BuildStep
    AdditionalBeanBuildItem registerTraceabilityObjectMapperProducer(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        if (traceabilityConfiguration.tracingMode().equals(TracingMode.INVOLVED)
                || traceabilityConfiguration.tracingMode().equals(TracingMode.INVOLVED_WITH_FULL_DETAILS)) {
            builder.addBeanClass(TraceabilityObjectMapperProducer.class);
        }
        return builder.build();
    }

    @BuildStep
    void registerTraceabilityFinderInvolvedEndpoint(final TraceabilityConfiguration traceabilityConfiguration,
                                                    final BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItemBuildProducer,
                                                    final BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        if (traceabilityConfiguration.tracingMode().equals(TracingMode.INVOLVED)
                || traceabilityConfiguration.tracingMode().equals(TracingMode.INVOLVED_WITH_FULL_DETAILS)) {
            additionalIndexedClassesBuildItemBuildProducer.produce(new AdditionalIndexedClassesBuildItem(
                    TraceabilityFinderInvolvedEndpoint.class.getName()));
            additionalBeanBuildItemBuildProducer.produce(AdditionalBeanBuildItem.builder().addBeanClasses(
                    TraceabilityFinderInvolvedEndpoint.class).build());
        }
    }

    @BuildStep
    void registerTraceabilityFinderDetailedInvolvedEndpoint(final TraceabilityConfiguration traceabilityConfiguration,
                                                            final BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItemBuildProducer,
                                                            final BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        if (traceabilityConfiguration.tracingMode().equals(TracingMode.INVOLVED_WITH_FULL_DETAILS)) {
            additionalIndexedClassesBuildItemBuildProducer.produce(new AdditionalIndexedClassesBuildItem(
                    TraceabilityFinderDetailedInvolvedEndpoint.class.getName()));
            additionalBeanBuildItemBuildProducer.produce(AdditionalBeanBuildItem.builder().addBeanClasses(
                    TraceabilityFinderDetailedInvolvedEndpoint.class).build());
        }
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem registerFinderExceptionMapper() {
        return new AdditionalIndexedClassesBuildItem(FinderExceptionMapper.class.getName());
    }

    @BuildStep
    AdditionalBeanBuildItem registerEncodedInvolvedRepository(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        if (TracingMode.INVOLVED.equals(traceabilityConfiguration.tracingMode())
                || TracingMode.INVOLVED_WITH_FULL_DETAILS.equals(traceabilityConfiguration.tracingMode())) {
            builder.addBeanClass(JdbcPostgresEncodedInvolvedRepository.class);
        }
        return builder.build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerEncodedDetailedInvolvedRepository(final TraceabilityConfiguration traceabilityConfiguration) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        if (TracingMode.INVOLVED_WITH_FULL_DETAILS.equals(traceabilityConfiguration.tracingMode())) {
            builder.addBeanClass(JdbcPostgresEncodedDetailedInvolvedRepository.class);
        }
        return builder.build();
    }
}
