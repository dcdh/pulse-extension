package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.PulseCommonProcessor;
import com.damdamdeo.pulse.extension.common.deployment.items.AdditionalVolumeBuildItem;
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
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

public class PulseConsumerProcessor {

    @BuildStep
    List<TargetBuildItem> discoverTargets(final List<ValidationErrorBuildItem> validationErrorBuildItems,
                                          final CombinedIndexBuildItem combinedIndexBuildItem) {
        if (validationErrorBuildItems.isEmpty()) {
            final IndexView computingIndex = combinedIndexBuildItem.getIndex();
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
    void generateCompose(final Capabilities capabilities,
                         final List<TargetBuildItem> targetBuildItems,
                         final BuildProducer<ComposeServiceBuildItem> composeServiceBuildItemBuildProducer,
                         final BuildProducer<AdditionalVolumeBuildItem> additionalVolumeBuildItemBuildProducer) {
        if (!capabilities.isPresent("com.damdamdeo.pulse-writer-extension")
                && !capabilities.isPresent("com.damdamdeo.pulse-publisher-extension")) {
            composeServiceBuildItemBuildProducer.produce(List.of(
                    PulseCommonProcessor.POSTGRES_COMPOSE_SERVICE_BUILD_ITEM,
                    PulseCommonProcessor.KAFKA_COMPOSE_SERVICE_BUILD_ITEM));

            final List<AdditionalVolumeBuildItem> additionalVolumeBuildItems = targetBuildItems.stream()
                    .flatMap(targetBuildItem -> targetBuildItem.sources().stream())
                    .distinct()
                    .map(ApplicationNaming::value)
                    .map(String::toLowerCase)
                    .map(schemaName ->
                            new AdditionalVolumeBuildItem(
                                    PulseCommonProcessor.POSTGRES_SERVICE_NAME,
                                    new ComposeServiceBuildItem.Volume(
                                            "./%s.sql".formatted(schemaName),
                                            "/docker-entrypoint-initdb.d/%s.sql".formatted(schemaName),
                                            // language=sql
                                            """
                                                    CREATE SCHEMA IF NOT EXISTS %1$s;
                                                    CREATE TABLE IF NOT EXISTS %1$s.t_aggregate_root (
                                                      aggregate_root_type character varying(255) not null,
                                                      aggregate_root_id character varying(255) not null,
                                                      last_version bigint not null,
                                                      aggregate_root_payload bytea NOT NULL CHECK (octet_length(aggregate_root_payload) <= 1000 * 1024),
                                                      owned_by character varying(255) not null,
                                                      belongs_to character varying(255) not null,
                                                      CONSTRAINT t_aggregate_root_pkey PRIMARY KEY (aggregate_root_id, aggregate_root_type)
                                                    );
                                                    """.formatted(schemaName).getBytes(StandardCharsets.UTF_8)
                                    )
                            )
                    ).toList();
            additionalVolumeBuildItemBuildProducer.produce(additionalVolumeBuildItems);
        }
    }
}
