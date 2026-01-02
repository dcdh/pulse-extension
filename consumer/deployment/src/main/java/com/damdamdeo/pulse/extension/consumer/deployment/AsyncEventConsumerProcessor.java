package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.PulseCommonProcessor;
import com.damdamdeo.pulse.extension.common.deployment.items.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.consumer.deployment.items.ConsumerChannelToValidateBuildItem;
import com.damdamdeo.pulse.extension.consumer.deployment.items.DiscoveredAsyncEventConsumerChannel;
import com.damdamdeo.pulse.extension.consumer.runtime.event.*;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;
import com.damdamdeo.pulse.extension.core.consumer.event.AbstractPurposeEventChannelConsumer;
import com.damdamdeo.pulse.extension.core.consumer.event.EventKey;
import com.damdamdeo.pulse.extension.core.consumer.event.EventValue;
import com.damdamdeo.pulse.extension.core.consumer.event.PurposeEventChannelExecutor;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyRepository;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.Topic;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.*;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.damdamdeo.pulse.extension.common.deployment.CodeGenerationWriter.writeGeneratedClass;
import static org.apache.commons.lang3.StringUtils.capitalize;

public class AsyncEventConsumerProcessor {

    private static final Topic TOPIC = Topic.EVENT;

    @BuildStep
    List<DiscoveredAsyncEventConsumerChannel> discoverAsyncEventConsumerChannels(final List<ValidationErrorBuildItem> validationErrorBuildItems,
                                                                                 final CombinedIndexBuildItem combinedIndexBuildItem) {
        if (validationErrorBuildItems.isEmpty()) {
            final IndexView computingIndex = combinedIndexBuildItem.getIndex();
            return computingIndex.getAnnotations(AsyncEventConsumerChannel.class)
                    .stream()
                    .map(annotationInstance -> {
                        final Purpose purpose = new Purpose(annotationInstance.value("purpose").asString());
                        final List<FromApplication> sources = annotationInstance.value("sources").asArrayList().stream()
                                .map(annotationValue -> {
                                    final AnnotationInstance nested = annotationValue.asNested();
                                    return FromApplication.of(
                                            nested.value("functionalDomain").asString(),
                                            nested.value("componentName").asString());
                                }).toList();
                        return new DiscoveredAsyncEventConsumerChannel(purpose, sources);
                    })
                    .distinct()
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    ConsumerChannelToValidateBuildItem channelToValidateBuildItemProducer() {
        return new ConsumerChannelToValidateBuildItem(AsyncEventConsumerChannel.class);
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans(final List<ValidationErrorBuildItem> validationErrorBuildItems) {
        if (validationErrorBuildItems.isEmpty()) {
            return Stream.of(
                            AsyncEventConsumerChannel.class,
                            PostgresAggregateRootLoader.class,
                            DefaultAsyncEventChannelMessageHandlerProvider.class,
                            JsonNodePurposeEventChannelExecutor.class)
                    .map(beanClazz -> AdditionalBeanBuildItem.builder().addBeanClass(beanClazz).build())
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalVolumeBuildItem> generateAdditionalVolumeBuildItems(final Capabilities capabilities,
                                                                       final List<DiscoveredAsyncEventConsumerChannel> discoveredAsyncEventConsumerChannels) {
        if (PulseConsumerProcessor.shouldGenerate(capabilities)) {
            return discoveredAsyncEventConsumerChannels.stream()
                    .flatMap(discoveredAsyncEventConsumerChannel -> discoveredAsyncEventConsumerChannel.sources().stream())
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
        } else {
            return List.of();
        }
    }

    @BuildStep
    void generateTargetsEventChannelConsumer(final List<DiscoveredAsyncEventConsumerChannel> discoveredAsyncEventConsumerChannels,
                                             final ApplicationInfoBuildItem applicationInfoBuildItem,
                                             final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                             final BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemBuildProducer,
                                             final OutputTargetBuildItem outputTargetBuildItem) {
        discoveredAsyncEventConsumerChannels.stream()
                .flatMap(discoveredAsyncEventConsumerChannel -> discoveredAsyncEventConsumerChannel.sources().stream()
                        .map(src -> new TargetWithSource(discoveredAsyncEventConsumerChannel.target(), src))
                ).forEach(targetWithSource -> {
                    final String className = AbstractPurposeEventChannelConsumer.class.getPackageName() + "."
                            + capitalize(targetWithSource.purpose().name())
                            + capitalize(targetWithSource.fromApplication().value())
                            + "TargetEventChannelConsumer";
                    try (final ClassCreator beanClassCreator = ClassCreator.builder()
                            .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                            .className(className)
                            .superClass(AbstractPurposeEventChannelConsumer.class)
                            .signature(SignatureBuilder.forClass()
                                    .setSuperClass(
                                            Type.parameterizedType(
                                                    Type.classType(AbstractPurposeEventChannelConsumer.class),
                                                    Type.classType(JsonNode.class)
                                            )))
                            .build()) {
                        beanClassCreator.addAnnotation(Singleton.class);
                        beanClassCreator.addAnnotation(Unremovable.class);
                        beanClassCreator.addAnnotation(DefaultBean.class);

                        try (final MethodCreator constructor = beanClassCreator.getMethodCreator("<init>", void.class,
                                PurposeEventChannelExecutor.class, IdempotencyRepository.class)) {
                            constructor
                                    .setSignature(SignatureBuilder.forMethod()
                                            .addParameterType(Type.parameterizedType(
                                                    Type.classType(PurposeEventChannelExecutor.class),
                                                    Type.classType(JsonNode.class)
                                            ))
                                            .addParameterType(Type.classType(IdempotencyRepository.class))
                                            .build());
                            constructor.setModifiers(Modifier.PUBLIC);

                            constructor.invokeSpecialMethod(
                                    MethodDescriptor.ofConstructor(AbstractPurposeEventChannelConsumer.class,
                                            PurposeEventChannelExecutor.class, IdempotencyRepository.class),
                                    constructor.getThis(),
                                    constructor.getMethodParam(0),
                                    constructor.getMethodParam(1));

                            constructor.returnValue(null);
                        }

                        try (final MethodCreator consume = beanClassCreator.getMethodCreator("consume", void.class, ConsumerRecord.class)) {
                            consume.setSignature(
                                    SignatureBuilder.forMethod()
                                            .addParameterType(Type.parameterizedType(
                                                    Type.classType(ConsumerRecord.class),
                                                    Type.classType(JsonNodeEventKey.class),
                                                    Type.classType(JsonNodeEventValue.class)
                                            ))
                                            .build());
                            consume.addAnnotation(Transactional.class);
                            consume.addAnnotation(Blocking.class);
                            consume.addAnnotation(Incoming.class).addValue("value", targetWithSource.channel(TOPIC));

                            final ResultHandle recordParam = consume.getMethodParam(0);
                            final ResultHandle targetParam = consume.newInstance(
                                    MethodDescriptor.ofConstructor(Purpose.class, String.class),
                                    consume.load(targetWithSource.purpose().name()));
                            final ResultHandle applicationNamingParam = consume.newInstance(
                                    MethodDescriptor.ofConstructor(FromApplication.class, String.class, String.class),
                                    consume.load(targetWithSource.fromApplication().functionalDomain()),
                                    consume.load(targetWithSource.fromApplication().componentName()));
                            final ResultHandle eventKeyParam = consume.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(ConsumerRecord.class, "key", Object.class), recordParam);
                            final ResultHandle eventValueParam = consume.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(ConsumerRecord.class, "value", Object.class), recordParam);
                            consume.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(
                                            className,
                                            "handleMessage",
                                            void.class,
                                            Purpose.class,
                                            FromApplication.class,
                                            EventKey.class,
                                            EventValue.class
                                    ),
                                    consume.getThis(),
                                    targetParam,
                                    applicationNamingParam,
                                    eventKeyParam,
                                    eventValueParam
                            );
                            consume.returnValue(null);
                        }
                        writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
                    }
                });

        discoveredAsyncEventConsumerChannels.stream()
                .flatMap(discoveredAsyncEventConsumerChannel -> discoveredAsyncEventConsumerChannel.sources().stream()
                        .map(src -> new TargetWithSource(discoveredAsyncEventConsumerChannel.target(), src))
                ).forEach(targetWithSource -> {
                    final String channelNaming = targetWithSource.channel(TOPIC);
                    final String topic = "pulse.%s_%s.t_event".formatted(
                            targetWithSource.fromApplication().functionalDomain().toLowerCase(),
                            targetWithSource.fromApplication().componentName().toLowerCase());
                    Map.of(
                                    "mp.messaging.incoming.%s.group.id".formatted(channelNaming), applicationInfoBuildItem.getName(),
                                    "mp.messaging.incoming.%s.enable.auto.commit".formatted(channelNaming), "true",
                                    "mp.messaging.incoming.%s.auto.offset.reset".formatted(channelNaming), "earliest",
                                    "mp.messaging.incoming.%s.connector".formatted(channelNaming), "smallrye-kafka",
                                    "mp.messaging.incoming.%s.topic".formatted(channelNaming), topic,
                                    "mp.messaging.incoming.%s.key.deserializer".formatted(channelNaming), JsonNodeEventKeyDeserializer.class.getName(),
                                    "mp.messaging.incoming.%s.value.deserializer".formatted(channelNaming), JsonNodeEventValueDeserializer.class.getName(),
                                    "mp.messaging.incoming.%s.value.deserializer.key-type".formatted(channelNaming), JsonNodeEventKey.class.getName(),
                                    "mp.messaging.incoming.%s.value.deserializer.value-type".formatted(channelNaming), JsonNodeEventValue.class.getName())
                            .forEach((key, value) -> runTimeConfigurationDefaultBuildItemBuildProducer.produce(new RunTimeConfigurationDefaultBuildItem(key, value)));
                });
    }
}
