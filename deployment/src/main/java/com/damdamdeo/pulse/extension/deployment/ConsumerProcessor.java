package com.damdamdeo.pulse.extension.deployment;

import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.runtime.PulseConfiguration;
import com.damdamdeo.pulse.extension.runtime.consumer.*;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.*;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.jandex.IndexView;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConsumerProcessor {

    public static final class TargetBuildItem extends MultiBuildItem {

        private final String target;

        public TargetBuildItem(final String target) {
            this.target = Objects.requireNonNull(target);
        }

        public String target() {
            return target;
        }
    }

    private static final Pattern TARGET_PATTERN = Pattern.compile("^[a-zA-Z]+$");

    private static final Pattern TOPIC_PATTERN = Pattern.compile("^[a-zA-Z]+$");

    @BuildStep
    List<ValidationErrorBuildItem> validateTargetNaming(final Capabilities capabilities,
                                                        final CombinedIndexBuildItem combinedIndexBuildItem) {
        if (capabilities.isPresent(Capability.KAFKA)) {
            final IndexView computingIndex = combinedIndexBuildItem.getComputingIndex();
            return computingIndex.getAnnotations(EventChannel.class)
                    .stream()
                    .map(annotationInstance -> annotationInstance.value("target").asString())
                    .filter(target -> !TARGET_PATTERN.matcher(target).matches())
                    .distinct()
                    .map(invalidTargetNaming -> new ValidationErrorBuildItem(
                            new IllegalStateException("Target naming invalid '%s' - it should match [a-zA-Z]+".formatted(invalidTargetNaming))))
                    .toList();
        }
        return List.of();
    }

    @BuildStep
    List<ValidationErrorBuildItem> validateNoTargetDuplication(final Capabilities capabilities,
                                                               final CombinedIndexBuildItem combinedIndexBuildItem) {
        if (capabilities.isPresent(Capability.KAFKA)) {
            final IndexView computingIndex = combinedIndexBuildItem.getComputingIndex();
            final Map<String, Long> targetCounts = computingIndex.getAnnotations(EventChannel.class)
                    .stream()
                    .map(annotationInstance -> annotationInstance.value("target").asString())
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            return targetCounts.entrySet().stream()
                    .filter(tc -> tc.getValue() > 1L)
                    .map(entry -> new ValidationErrorBuildItem(
                            new IllegalStateException("Target '%s' declared more than once '%d'".formatted(entry.getKey(), entry.getValue()))))
                    .toList();
        }
        return List.of();
    }

    @BuildStep
    List<TargetBuildItem> discoverTargets(final Capabilities capabilities,
                                          final List<ValidationErrorBuildItem> validationErrorBuildItems,
                                          final CombinedIndexBuildItem combinedIndexBuildItem) {
        if (capabilities.isPresent(Capability.KAFKA) && validationErrorBuildItems.isEmpty()) {
            final IndexView computingIndex = combinedIndexBuildItem.getComputingIndex();
            return computingIndex.getAnnotations(EventChannel.class)
                    .stream()
                    .map(annotationInstance -> annotationInstance.value("target").asString())
                    .distinct()
                    .map(TargetBuildItem::new)
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans(final Capabilities capabilities,
                                                  final List<ValidationErrorBuildItem> validationErrorBuildItems) {
        if (capabilities.isPresent(Capability.KAFKA)) {
            return Stream.of(
                            DebeziumConfigurator.class,
                            JdbcPostgresIdempotencyRepository.class,
                            PostgresAggregateRootLoader.class,
                            JacksonDecryptedPayloadToPayloadMapper.class,
                            DefaultAsyncEventChannelMessageHandlerProvider.class)
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
    List<ValidationErrorBuildItem> validateTargetBindingWithTopic(final Capabilities capabilities,
                                                                  final CombinedIndexBuildItem combinedIndexBuildItem,
                                                                  final PulseConfiguration pulseConfiguration) {
        if (capabilities.isPresent(Capability.KAFKA)) {
            final IndexView computingIndex = combinedIndexBuildItem.getComputingIndex();

            return computingIndex.getAnnotations(EventChannel.class)
                    .stream()
                    .map(annotationInstance -> annotationInstance.value("target").asString())
                    .map(targetName -> {
                        final String topic = pulseConfiguration.targetTopicBinding().get(targetName);
                        if (topic != null) {
                            if (!TOPIC_PATTERN.matcher(topic).matches()) {
                                return new ValidationErrorBuildItem(
                                        new IllegalStateException("Topic naming invalid '%s' - it should match [a-zA-Z]+".formatted(topic)));
                            } else {
                                return null;
                            }
                        } else {
                            return new ValidationErrorBuildItem(
                                    new IllegalStateException("Missing topic binding for target '%s'".formatted(targetName)));
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }
        return List.of();
    }


    @BuildStep
    void generateTargetsEventChannelConsumer(final List<TargetBuildItem> targetBuildItems,
                                             final PulseConfiguration pulseConfiguration,
                                             final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                             final BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemBuildProducer,
                                             final OutputTargetBuildItem outputTargetBuildItem) {
        targetBuildItems.forEach(targetBuildItem -> {
            final String targetNaming = targetBuildItem.target();
            final String className = AbstractTargetEventChannelConsumer.class.getPackageName() + "." + capitalize(targetNaming) + "TargetEventChannelConsumer";
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
                        DecryptionService.class, DecryptedPayloadToPayloadMapper.class, AggregateRootLoader.class,
                        AsyncEventChannelMessageHandlerProvider.class, IdempotencyRepository.class,
                        SequentialEventChecker.class)) {
                    constructor
                            .setSignature(SignatureBuilder.forMethod()
                                    .addParameterType(Type.classType(DecryptionService.class))
                                    .addParameterType(Type.parameterizedType(
                                            Type.classType(DecryptedPayloadToPayloadMapper.class),
                                            Type.classType(JsonNode.class)
                                    ))
                                    .addParameterType(Type.parameterizedType(
                                            Type.classType(AggregateRootLoader.class),
                                            Type.classType(JsonNode.class)
                                    ))
                                    .addParameterType(Type.parameterizedType(
                                            Type.classType(AsyncEventChannelMessageHandlerProvider.class),
                                            Type.classType(JsonNode.class)
                                    ))
                                    .addParameterType(Type.classType(IdempotencyRepository.class))
                                    .addParameterType(Type.classType(SequentialEventChecker.class))
                                    .build());
                    constructor.setModifiers(Modifier.PUBLIC);

                    constructor.invokeSpecialMethod(
                            MethodDescriptor.ofConstructor(AbstractTargetEventChannelConsumer.class,
                                    DecryptionService.class, DecryptedPayloadToPayloadMapper.class, AggregateRootLoader.class,
                                    AsyncEventChannelMessageHandlerProvider.class, IdempotencyRepository.class,
                                    SequentialEventChecker.class),
                            constructor.getThis(),
                            constructor.getMethodParam(0),
                            constructor.getMethodParam(1),
                            constructor.getMethodParam(2),
                            constructor.getMethodParam(3),
                            constructor.getMethodParam(4),
                            constructor.getMethodParam(5));

                    constructor.returnValue(null);
                }

                try (final MethodCreator consume = beanClassCreator.getMethodCreator("consume", void.class, Record.class)) {
                    consume.setSignature(
                            SignatureBuilder.forMethod()
                                    .addParameterType(Type.parameterizedType(
                                            Type.classType(Record.class),
                                            Type.classType(EventKey.class),
                                            Type.classType(EventRecord.class)
                                    ))
                                    .build());
                    consume.addAnnotation(Transactional.class);
                    consume.addAnnotation(Blocking.class);
                    consume.addAnnotation(Incoming.class).addValue("value", targetNaming);

                    final ResultHandle record = consume.getMethodParam(0);
                    final ResultHandle target = consume.newInstance(
                            MethodDescriptor.ofConstructor(Target.class, String.class),
                            consume.load(targetNaming));
                    final ResultHandle eventKey = consume.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(Record.class, "key", EventKey.class), record);
                    final ResultHandle eventRecord = consume.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(Record.class, "value", EventRecord.class), record);

                    consume.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(
                                    className,
                                    "handleMessage",
                                    void.class,
                                    Target.class,
                                    EventKey.class,
                                    EventRecord.class
                            ),
                            consume.getThis(),
                            target,
                            eventKey,
                            eventRecord
                    );
                    consume.returnValue(null);
                }
                CodeGenerationProcessor.writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
            }
        });

        for (final TargetBuildItem targetBuildItem : targetBuildItems) {
            final String target = targetBuildItem.target();
            Map.of(
                            "mp.messaging.incoming.%s.enable.auto.commit".formatted(target), "true",
                            "mp.messaging.incoming.%s.auto.offset.reset".formatted(target), "earliest",
                            "mp.messaging.incoming.%s.connector".formatted(target), "smallrye-kafka",
                            "mp.messaging.incoming.%s.topic".formatted(target), "%s_t_event".formatted(pulseConfiguration.targetTopicBinding().get(target)),
                            "mp.messaging.incoming.%s.key.deserializer".formatted(target), JsonNodeEventKeyObjectMapperDeserializer.class.getName(),
                            "mp.messaging.incoming.%s.value.deserializer".formatted(target), JsonNodeEventRecordObjectMapperDeserializer.class.getName(),
                            "mp.messaging.incoming.%s.value.deserializer.key-type".formatted(target), EventKey.class.getName(),
                            "mp.messaging.incoming.%s.value.deserializer.value-type".formatted(target), EventRecord.class.getName())
                    .forEach((key, value) -> runTimeConfigurationDefaultBuildItemBuildProducer.produce(new RunTimeConfigurationDefaultBuildItem(key, value)));
        }
    }

    private static String capitalize(final String input) {
        if (input == null || input.isBlank()) return input;
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
