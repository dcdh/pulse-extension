package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.PostgresSqlScriptBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.consumer.deployment.items.ConsumerChannelToValidateBuildItem;
import com.damdamdeo.pulse.extension.consumer.deployment.items.DiscoveredAsyncAggregateRootConsumerChannel;
import com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot.*;
import com.damdamdeo.pulse.extension.core.consumer.CdcTopicNaming;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;
import com.damdamdeo.pulse.extension.core.consumer.Table;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.AbstractPurposeAggregateRootChannelConsumer;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.AggregateRootKey;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.AggregateRootValue;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.PurposeAggregateRootChannelExecutor;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.IdempotencyRepository;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.damdamdeo.pulse.extension.common.deployment.CodeGenerationWriter.writeGeneratedClass;
import static org.apache.commons.lang3.StringUtils.capitalize;

public class AsyncAggregateRootConsumerProcessor {

    private static final Table TABLE = Table.AGGREGATE_ROOT;

    @BuildStep
    List<DiscoveredAsyncAggregateRootConsumerChannel> discoverAsyncEventConsumerChannels(final List<ValidationErrorBuildItem> validationErrorBuildItems,
                                                                                         final CombinedIndexBuildItem combinedIndexBuildItem) {
        if (validationErrorBuildItems.isEmpty()) {
            final IndexView computingIndex = combinedIndexBuildItem.getIndex();
            return computingIndex.getAnnotations(AsyncAggregateRootConsumerChannel.class)
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
                        return new DiscoveredAsyncAggregateRootConsumerChannel(purpose, sources);
                    })
                    .distinct()
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    ConsumerChannelToValidateBuildItem channelToValidateBuildItemProducer() {
        return new ConsumerChannelToValidateBuildItem(AsyncAggregateRootConsumerChannel.class);
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans(final List<ValidationErrorBuildItem> validationErrorBuildItems) {
        if (validationErrorBuildItems.isEmpty()) {
            return Stream.of(
                            AsyncAggregateRootConsumerChannel.class,
                            DefaultAsyncAggregateRootChannelMessageHandlerProvider.class,
                            JsonNodePurposeAggregateRootChannelExecutor.class)
                    .map(beanClazz -> AdditionalBeanBuildItem.builder().addBeanClass(beanClazz).build())
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<PostgresSqlScriptBuildItem> generatePostgresSqlScriptBuildItems(final Capabilities capabilities,
                                                                         final List<DiscoveredAsyncAggregateRootConsumerChannel> discoveredAsyncAggregateRootConsumerChannels) {
        if (PulseConsumerProcessor.shouldGenerate(capabilities)) {
            return discoveredAsyncAggregateRootConsumerChannels.stream()
                    .flatMap(discoveredAsyncAggregateRootConsumerChannel -> discoveredAsyncAggregateRootConsumerChannel.sources().stream())
                    .distinct()
                    .map(FromApplication::value)
                    .map(String::toLowerCase)
                    .map(schemaName -> new PostgresSqlScriptBuildItem(
                                    "%s_target_consumer.sql".formatted(schemaName),
                                    // language=sql
                                    """
                                            CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
                                            CREATE SCHEMA IF NOT EXISTS %1$s;
                                            
                                            CREATE TABLE IF NOT EXISTS %1$s.aggregate_root (
                                              aggregate_root_type character varying(255) not null,
                                              aggregate_root_id character varying(255) not null,
                                              last_version bigint not null,
                                              aggregate_root_payload bytea NOT NULL CHECK (octet_length(aggregate_root_payload) <= 1000 * 1024),
                                              owned_by character varying(255) not null,
                                              belongs_to character varying(255) not null,
                                              CONSTRAINT aggregate_root_pkey PRIMARY KEY (aggregate_root_id, aggregate_root_type)
                                            );
                                            """.formatted(schemaName)
                            )
                    ).toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    void generateTargetsAggregateRootChannelConsumer(final List<DiscoveredAsyncAggregateRootConsumerChannel> discoveredAsyncAggregateRootConsumerChannels,
                                                     final ApplicationInfoBuildItem applicationInfoBuildItem,
                                                     final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                                     final BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemBuildProducer,
                                                     final OutputTargetBuildItem outputTargetBuildItem) {
        discoveredAsyncAggregateRootConsumerChannels.stream()
                .flatMap(discoveredAsyncAggregateRootConsumerChannel -> discoveredAsyncAggregateRootConsumerChannel.sources().stream()
                        .map(src -> new TargetWithSource(discoveredAsyncAggregateRootConsumerChannel.target(), src))
                ).forEach(targetWithSource -> {
                    final String className = AbstractPurposeAggregateRootChannelConsumer.class.getPackageName() + "."
                            + capitalize(targetWithSource.purpose().name())
                            + capitalize(targetWithSource.fromApplication().value())
                            + "TargetAggregateRootChannelConsumer";
                    try (final ClassCreator beanClassCreator = ClassCreator.builder()
                            .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                            .className(className)
                            .superClass(AbstractPurposeAggregateRootChannelConsumer.class)
                            .signature(SignatureBuilder.forClass()
                                    .setSuperClass(
                                            Type.parameterizedType(
                                                    Type.classType(AbstractPurposeAggregateRootChannelConsumer.class),
                                                    Type.classType(JsonNode.class)
                                            )))
                            .build()) {
                        beanClassCreator.addAnnotation(Singleton.class);
                        beanClassCreator.addAnnotation(Unremovable.class);
                        beanClassCreator.addAnnotation(DefaultBean.class);

                        try (final MethodCreator constructor = beanClassCreator.getMethodCreator("<init>", void.class,
                                PurposeAggregateRootChannelExecutor.class, IdempotencyRepository.class)) {
                            constructor
                                    .setSignature(SignatureBuilder.forMethod()
                                            .addParameterType(Type.parameterizedType(
                                                    Type.classType(PurposeAggregateRootChannelExecutor.class),
                                                    Type.classType(JsonNode.class)
                                            ))
                                            .addParameterType(Type.classType(IdempotencyRepository.class))
                                            .build());
                            constructor.setModifiers(Modifier.PUBLIC);

                            constructor.invokeSpecialMethod(
                                    MethodDescriptor.ofConstructor(AbstractPurposeAggregateRootChannelConsumer.class,
                                            PurposeAggregateRootChannelExecutor.class, IdempotencyRepository.class),
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
                                                    Type.classType(JsonNodeAggregateRootKey.class),
                                                    Type.classType(JsonNodeAggregateRootValue.class)
                                            ))
                                            .build());
                            consume.addAnnotation(Transactional.class);
                            consume.addAnnotation(Blocking.class);
                            consume.addAnnotation(Incoming.class).addValue("value", targetWithSource.channel(TABLE));

                            final ResultHandle recordParam = consume.getMethodParam(0);
                            final ResultHandle targetParam = consume.newInstance(
                                    MethodDescriptor.ofConstructor(Purpose.class, String.class),
                                    consume.load(targetWithSource.purpose().name()));
                            final ResultHandle applicationNamingParam = consume.newInstance(
                                    MethodDescriptor.ofConstructor(FromApplication.class, String.class, String.class),
                                    consume.load(targetWithSource.fromApplication().functionalDomain()),
                                    consume.load(targetWithSource.fromApplication().componentName()));
                            final ResultHandle aggregateRootKeyParam = consume.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(ConsumerRecord.class, "key", Object.class), recordParam);
                            final ResultHandle aggregateRootValueParam = consume.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(ConsumerRecord.class, "value", Object.class), recordParam);
                            consume.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(
                                            className,
                                            "handleMessage",
                                            void.class,
                                            Purpose.class,
                                            FromApplication.class,
                                            AggregateRootKey.class,
                                            AggregateRootValue.class
                                    ),
                                    consume.getThis(),
                                    targetParam,
                                    applicationNamingParam,
                                    aggregateRootKeyParam,
                                    aggregateRootValueParam
                            );
                            consume.returnValue(null);
                        }
                        writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
                    }
                });

        discoveredAsyncAggregateRootConsumerChannels.stream()
                .flatMap(discoveredAsyncAggregateRootConsumerChannel -> discoveredAsyncAggregateRootConsumerChannel.sources().stream()
                        .map(src -> new TargetWithSource(discoveredAsyncAggregateRootConsumerChannel.target(), src))
                ).forEach(targetWithSource -> {
                    final String channelNaming = targetWithSource.channel(TABLE);
                    final String topic = new CdcTopicNaming(targetWithSource.fromApplication(), Table.AGGREGATE_ROOT).name();
                    Map.of(
                                    "mp.messaging.incoming.%s.group.id".formatted(channelNaming), applicationInfoBuildItem.getName(),
                                    "mp.messaging.incoming.%s.enable.auto.commit".formatted(channelNaming), "true",
                                    "mp.messaging.incoming.%s.auto.offset.reset".formatted(channelNaming), "earliest",
                                    "mp.messaging.incoming.%s.connector".formatted(channelNaming), "smallrye-kafka",
                                    "mp.messaging.incoming.%s.topic".formatted(channelNaming), topic,
                                    "mp.messaging.incoming.%s.key.deserializer".formatted(channelNaming), JsonNodeAggregateRootKeyDeserializer.class.getName(),
                                    "mp.messaging.incoming.%s.value.deserializer".formatted(channelNaming), JsonNodeAggregateRootValueDeserializer.class.getName(),
                                    "mp.messaging.incoming.%s.value.deserializer.key-type".formatted(channelNaming), JsonNodeAggregateRootKey.class.getName(),
                                    "mp.messaging.incoming.%s.value.deserializer.value-type".formatted(channelNaming), JsonNodeAggregateRootValue.class.getName())
                            .forEach((key, value) -> runTimeConfigurationDefaultBuildItemBuildProducer.produce(new RunTimeConfigurationDefaultBuildItem(key, value)));
                });
    }
}
