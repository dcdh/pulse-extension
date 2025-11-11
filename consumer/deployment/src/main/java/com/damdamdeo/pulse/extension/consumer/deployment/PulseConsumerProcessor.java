package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.PulseCommonProcessor;
import com.damdamdeo.pulse.extension.common.deployment.items.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.consumer.deployment.items.TargetBuildItem;
import com.damdamdeo.pulse.extension.consumer.runtime.*;
import com.damdamdeo.pulse.extension.core.consumer.ApplicationNaming;
import com.damdamdeo.pulse.extension.core.consumer.SequentialEventChecker;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import java.util.List;
import java.util.stream.Stream;

public class PulseConsumerProcessor {

    @BuildStep
    List<TargetBuildItem> discoverTargets(final List<ValidationErrorBuildItem> validationErrorBuildItems,
                                          final CombinedIndexBuildItem combinedIndexBuildItem) {
        if (validationErrorBuildItems.isEmpty()) {
            final IndexView computingIndex = combinedIndexBuildItem.getComputingIndex();
            return computingIndex.getAnnotations(EventChannel.class)
                    .stream()
                    .map(annotationInstance -> {
                        final Target target = new Target(annotationInstance.value("target").asString());
                        final List<ApplicationNaming> sources = annotationInstance.value("sources").asArrayList().stream()
                                .map(annotationValue -> {
                                    final AnnotationInstance nested = annotationValue.asNested();
                                    return ApplicationNaming.of(
                                            nested.value("functionalDomain").asString(),
                                            nested.value("componentName").asString());
                                }).toList();
                        return new TargetBuildItem(target, sources);
                    })
                    .distinct()
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans(final List<ValidationErrorBuildItem> validationErrorBuildItems) {
        if (validationErrorBuildItems.isEmpty()) {
            return Stream.of(
                            EventChannel.class,
                            JdbcPostgresIdempotencyRepository.class,
                            PostgresAggregateRootLoader.class,
                            JacksonDecryptedPayloadToPayloadMapper.class,
                            DefaultAsyncEventChannelMessageHandlerProvider.class,
                            JsonNodeTargetEventChannelExecutor.class)
                    .map(beanClazz -> AdditionalBeanBuildItem.builder().addBeanClass(beanClazz).build())
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    AdditionalBeanBuildItem registerSequentialEventChecker() {
        return AdditionalBeanBuildItem.builder().addBeanClass(SequentialEventChecker.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build();
    }

    @BuildStep
    List<ComposeServiceBuildItem> generateCompose(final Capabilities capabilities) {
        if (!capabilities.isPresent("com.damdamdeo.pulse-writer-extension")
                && !capabilities.isPresent("com.damdamdeo.pulse-publisher-extension")) {
            return List.of(
                    PulseCommonProcessor.POSTGRES_COMPOSE_SERVICE_BUILD_ITEM,
                    PulseCommonProcessor.KAFKA_COMPOSE_SERVICE_BUILD_ITEM);
        } else {
            return List.of();
        }
    }
}
