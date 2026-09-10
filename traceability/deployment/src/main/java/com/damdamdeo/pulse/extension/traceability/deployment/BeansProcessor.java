package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresExecutedByEncodedRepository;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresInvolvedTraceRecorderRepository;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresOwnedByProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class BeansProcessor {

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
            case DISABLED -> builder.addBeanClass(NoOpDetailedInvolvedFinder.class);
            case INVOLVED, INVOLVED_WITH_FULL_DETAILS -> builder.addBeanClass(DefaultDetailedInvolvedFinder.class);
        }
        return builder.build();
    }

//    FCK faire la creation des tables en fonction du  context ! puis faire dodo !!!
//    ha merde j'ai l'api rest à faire aussi !!!
}
