package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.PulseCommonProcessor;
import com.damdamdeo.pulse.extension.common.deployment.items.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.consumer.runtime.*;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.SequentialEventChecker;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
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
                        final List<FromApplication> sources = annotationInstance.value("sources").asArrayList().stream()
                                .map(annotationValue -> {
                                    final AnnotationInstance nested = annotationValue.asNested();
                                    return FromApplication.of(
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
                         final ApplicationInfoBuildItem applicationInfoBuildItem,
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
                    .map(FromApplication::value)
                    .map(String::toLowerCase)
                    .map(schemaName -> {
                                final String sqlFileName = "%s_target_consumer.sql".formatted(schemaName);
                                return new AdditionalVolumeBuildItem(
                                        PulseCommonProcessor.POSTGRES_SERVICE_NAME,
                                        new ComposeServiceBuildItem.Volume(
                                                "./%s".formatted(sqlFileName),
                                                "/docker-entrypoint-initdb.d/%s".formatted(sqlFileName),
                                                // language=sql
                                                """
                                                        CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
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
                                                        """.formatted(schemaName).getBytes(
                                                        StandardCharsets.UTF_8)
                                        )
                                );
                            }
                    ).toList();
            additionalVolumeBuildItemBuildProducer.produce(additionalVolumeBuildItems);
        }
        final String schemaName = applicationInfoBuildItem.getName().toLowerCase();
        final String sqlFileName = "%s_idempotency_consumer.sql".formatted(schemaName);
        AdditionalVolumeBuildItem additionalVolumeBuildItem = new AdditionalVolumeBuildItem(PulseCommonProcessor.POSTGRES_SERVICE_NAME,
                new ComposeServiceBuildItem.Volume(
                        "./%s".formatted(sqlFileName),
                        "/docker-entrypoint-initdb.d/%s".formatted(sqlFileName),
                        // language=sql
                        """
                                CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
                                CREATE SCHEMA IF NOT EXISTS %1$s;
                                
                                CREATE TABLE IF NOT EXISTS %1$s.t_idempotency (
                                  target character varying(255) not null,
                                  from_application character varying(255) not null,
                                  aggregate_root_type character varying(255) not null,
                                  aggregate_root_id character varying(255) not null,
                                  last_consumed_version bigint not null,
                                  PRIMARY KEY (target, from_application, aggregate_root_type, aggregate_root_id)
                                )
                                """.formatted(schemaName).getBytes(StandardCharsets.UTF_8)));
        additionalVolumeBuildItemBuildProducer.produce(additionalVolumeBuildItem);
    }

    @BuildStep
    ReflectiveClassBuildItem reflectiveClassBuildItem() {
        return ReflectiveClassBuildItem
                .builder(JsonNodeEventRecordObjectMapperDeserializer.class, JsonNodeEventKeyObjectMapperDeserializer.class,
                        JsonNodeEventKey.class, JsonNodeEventValue.class)
                .constructors().build();
    }
}
