package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.consumer.deployment.items.TargetBuildItem;
import com.damdamdeo.pulse.extension.consumer.runtime.JsonNodeEventKey;
import com.damdamdeo.pulse.extension.consumer.runtime.JsonNodeEventKeyObjectMapperDeserializer;
import com.damdamdeo.pulse.extension.consumer.runtime.JsonNodeEventRecordObjectMapperDeserializer;
import com.damdamdeo.pulse.extension.consumer.runtime.JsonNodeEventValue;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.*;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CodeGenerationProcessor {

    record TargetWithSource(Target target, FromApplication fromApplication) {

        public TargetWithSource {
            Objects.requireNonNull(target);
            Objects.requireNonNull(fromApplication);
        }

        public String channel() {
            return "%s-%s-%s-in".formatted(target.name().toLowerCase(),
                    fromApplication.functionalDomain().toLowerCase(),
                    fromApplication.componentName().toLowerCase());
        }
    }

    @BuildStep
    void generateTargetsEventChannelConsumer(final List<TargetBuildItem> targetBuildItems,
                                             final ApplicationInfoBuildItem applicationInfoBuildItem,
                                             final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                             final BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemBuildProducer,
                                             final OutputTargetBuildItem outputTargetBuildItem) {
        targetBuildItems.stream()
                .flatMap(targetBuildItem -> targetBuildItem.sources().stream()
                        .map(src -> new TargetWithSource(targetBuildItem.target(), src))
                ).forEach(targetWithSource -> {
                    final String className = AbstractTargetEventChannelConsumer.class.getPackageName() + "."
                            + capitalize(targetWithSource.target().name())
                            + capitalize(targetWithSource.fromApplication().value())
                            + "TargetEventChannelConsumer";
                    try (final ClassCreator beanClassCreator = ClassCreator.builder()
                            .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                            .className(className)
                            .superClass(AbstractTargetEventChannelConsumer.class)
                            .signature(SignatureBuilder.forClass()
                                    .setSuperClass(
                                            Type.parameterizedType(
                                                    Type.classType(AbstractTargetEventChannelConsumer.class),
                                                    Type.classType(JsonNode.class)
                                            )))
                            .build()) {
                        beanClassCreator.addAnnotation(Singleton.class);
                        beanClassCreator.addAnnotation(Unremovable.class);
                        beanClassCreator.addAnnotation(DefaultBean.class);

                        try (final MethodCreator constructor = beanClassCreator.getMethodCreator("<init>", void.class,
                                TargetEventChannelExecutor.class, IdempotencyRepository.class)) {
                            constructor
                                    .setSignature(SignatureBuilder.forMethod()
                                            .addParameterType(Type.parameterizedType(
                                                    Type.classType(TargetEventChannelExecutor.class),
                                                    Type.classType(JsonNode.class)
                                            ))
                                            .addParameterType(Type.classType(IdempotencyRepository.class))
                                            .build());
                            constructor.setModifiers(Modifier.PUBLIC);

                            constructor.invokeSpecialMethod(
                                    MethodDescriptor.ofConstructor(AbstractTargetEventChannelConsumer.class,
                                            TargetEventChannelExecutor.class, IdempotencyRepository.class),
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
                            consume.addAnnotation(Incoming.class).addValue("value", targetWithSource.channel());

                            final ResultHandle recordParam = consume.getMethodParam(0);
                            final ResultHandle targetParam = consume.newInstance(
                                    MethodDescriptor.ofConstructor(Target.class, String.class),
                                    consume.load(targetWithSource.target().name()));
                            final ResultHandle applicationNamingParam = consume.newInstance(
                                    MethodDescriptor.ofConstructor(FromApplication.class, String.class, String.class),
                                    consume.load(targetWithSource.fromApplication().functionalDomain()),
                                    consume.load(targetWithSource.fromApplication().componentName()));
                            final ResultHandle eventKeyParam = consume.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(ConsumerRecord.class, "key", Object.class), recordParam);
                            final ResultHandle eventRecordParam = consume.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(ConsumerRecord.class, "value", Object.class), recordParam);
                            consume.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(
                                            className,
                                            "handleMessage",
                                            void.class,
                                            Target.class,
                                            FromApplication.class,
                                            EventKey.class,
                                            EventValue.class
                                    ),
                                    consume.getThis(),
                                    targetParam,
                                    applicationNamingParam,
                                    eventKeyParam,
                                    eventRecordParam
                            );
                            consume.returnValue(null);
                        }
                        writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
                    }
                });

        targetBuildItems.stream()
                .flatMap(targetBuildItem -> targetBuildItem.sources().stream()
                        .map(src -> new TargetWithSource(targetBuildItem.target(), src))
                ).forEach(targetWithSource -> {
                    final String channelNaming = targetWithSource.channel();
                    final String topic = "pulse.%s_%s.t_event".formatted(
                            targetWithSource.fromApplication().functionalDomain().toLowerCase(),
                            targetWithSource.fromApplication().componentName().toLowerCase());
                    Map.of(
                                    "mp.messaging.incoming.%s.group.id".formatted(channelNaming), applicationInfoBuildItem.getName(),
                                    "mp.messaging.incoming.%s.enable.auto.commit".formatted(channelNaming), "true",
                                    "mp.messaging.incoming.%s.auto.offset.reset".formatted(channelNaming), "earliest",
                                    "mp.messaging.incoming.%s.connector".formatted(channelNaming), "smallrye-kafka",
                                    "mp.messaging.incoming.%s.topic".formatted(channelNaming), topic,
                                    "mp.messaging.incoming.%s.key.deserializer".formatted(channelNaming), JsonNodeEventKeyObjectMapperDeserializer.class.getName(),
                                    "mp.messaging.incoming.%s.value.deserializer".formatted(channelNaming), JsonNodeEventRecordObjectMapperDeserializer.class.getName(),
                                    "mp.messaging.incoming.%s.value.deserializer.key-type".formatted(channelNaming), EventKey.class.getName(),
                                    "mp.messaging.incoming.%s.value.deserializer.value-type".formatted(channelNaming), EventValue.class.getName())
                            .forEach((key, value) -> runTimeConfigurationDefaultBuildItemBuildProducer.produce(new RunTimeConfigurationDefaultBuildItem(key, value)));
                });
    }

    private static String capitalize(final String input) {
        if (input == null || input.isBlank()) return input;
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    public static void writeGeneratedClass(final ClassCreator classCreator, final OutputTargetBuildItem outputTargetBuildItem) {
        classCreator.writeTo((name, data) -> {
            final Path classGeneratedPath = outputTargetBuildItem.getOutputDirectory().resolve(name.substring(name.lastIndexOf("/") + 1) + ".class");
            try {
                if (Files.notExists(classGeneratedPath)) {
                    Files.createFile(classGeneratedPath);
                }
                Files.write(classGeneratedPath, data, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
