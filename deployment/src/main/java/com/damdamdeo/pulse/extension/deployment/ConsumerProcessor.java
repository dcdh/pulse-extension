package com.damdamdeo.pulse.extension.deployment;

import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.runtime.consumer.*;
import com.damdamdeo.pulse.extension.runtime.consumer.debezium.DebeziumConfigurator;
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
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConsumerProcessor {

    public static final class TargetBuildItem extends MultiBuildItem {

        private final Target target;
        private final List<ApplicationNaming> sources;

        public TargetBuildItem(final Target target, final List<ApplicationNaming> sources) {
            this.target = Objects.requireNonNull(target);
            this.sources = Objects.requireNonNull(sources);
        }

        public Target target() {
            return target;
        }

        public List<ApplicationNaming> sources() {
            return sources;
        }
    }

    record TargetWithSource(Target target, ApplicationNaming source) {

        public TargetWithSource {
            Objects.requireNonNull(target);
            Objects.requireNonNull(source);
        }

        public String channel() {
            return "%s-%s-%s".formatted(target.name().toLowerCase(),
                    source.functionalDomain().toLowerCase(),
                    source.componentName().toLowerCase());
        }
    }

    record InvalidNaming(String naming, Pattern pattern, Kind kind) implements InvalidMessage {

        enum Kind {
            TARGET,
            FUNCTIONAL_NAME,
            COMPONENT_NAME
        }

        public InvalidNaming {
            Objects.requireNonNull(naming);
            Objects.requireNonNull(pattern);
            Objects.requireNonNull(kind);
        }

        public static InvalidNaming ofTarget(final String naming, final Pattern pattern) {
            return new InvalidNaming(naming, pattern, Kind.TARGET);
        }

        public static InvalidNaming ofFunctional(final String naming, final Pattern pattern) {
            return new InvalidNaming(naming, pattern, Kind.FUNCTIONAL_NAME);
        }

        public static InvalidNaming ofComponent(final String naming, final Pattern pattern) {
            return new InvalidNaming(naming, pattern, Kind.COMPONENT_NAME);
        }

        @Override
        public String invalidMessage() {
            return switch (kind) {
                case Kind.TARGET ->
                        "Invalid Target name '%s' - it should match '%s'".formatted(naming, pattern.pattern());
                case Kind.FUNCTIONAL_NAME ->
                        "Invalid Functional name '%s' - it should match '%s'".formatted(naming, pattern.pattern());
                case Kind.COMPONENT_NAME ->
                        "Invalid Component name '%s' - it should match '%s'".formatted(naming, pattern.pattern());
            };
        }
    }

    interface InvalidMessage {

        String invalidMessage();
    }

    interface InvalidDuplicates extends InvalidMessage {

    }

    record InvalidTargetDuplicates(String naming, Long count) implements InvalidDuplicates {

        InvalidTargetDuplicates {
            Objects.requireNonNull(naming);
            Objects.requireNonNull(count);
        }

        @Override
        public String invalidMessage() {
            return "Target '%s' declared more than once '%d'".formatted(naming, count);
        }
    }

    record InvalidSourceDuplicates(String target, String functionalDomain, String componentName,
                                   Long count) implements InvalidDuplicates {

        InvalidSourceDuplicates {
            Objects.requireNonNull(target);
            Objects.requireNonNull(functionalDomain);
            Objects.requireNonNull(componentName);
            Objects.requireNonNull(count);
        }

        @Override
        public String invalidMessage() {
            return "functionalDomain '%s' componentName '%s' declared more than once '%d' in target '%s'"
                    .formatted(functionalDomain, componentName, count, target);
        }
    }

    record PreValidation(List<PreValidationEventChannel> preValidationEventChannels) {

        PreValidation {
            Objects.requireNonNull(preValidationEventChannels);
        }

        public List<InvalidNaming> invalidNamings() {
            final List<InvalidNaming> invalidNamings = new ArrayList<>();
            for (final PreValidationEventChannel preValidationEventChannel : preValidationEventChannels) {
                if (!Target.TARGET_PATTERN.matcher(preValidationEventChannel.target()).matches()) {
                    invalidNamings.add(InvalidNaming.ofTarget(preValidationEventChannel.target(), Target.TARGET_PATTERN));
                }
                for (final PreValidationSource preValidationSource : preValidationEventChannel.sources()) {
                    if (!ApplicationNaming.PART_PATTERN.matcher(preValidationSource.functionalDomain()).matches()) {
                        invalidNamings.add(InvalidNaming.ofFunctional(preValidationSource.functionalDomain(), ApplicationNaming.PART_PATTERN));
                    }
                    if (!ApplicationNaming.PART_PATTERN.matcher(preValidationSource.componentName()).matches()) {
                        invalidNamings.add(InvalidNaming.ofComponent(preValidationSource.componentName(), ApplicationNaming.PART_PATTERN));
                    }
                }
            }
            return invalidNamings;
        }

        public List<InvalidDuplicates> invalidDuplicates() {
            final List<InvalidDuplicates> invalidDuplicates = new ArrayList<>();

            preValidationEventChannels
                    .stream()
                    .map(PreValidationEventChannel::target)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet().stream()
                    .filter(tc -> tc.getValue() > 1L)
                    .forEach(tc -> invalidDuplicates.add(
                            new InvalidTargetDuplicates(tc.getKey(), tc.getValue())));

            for (final PreValidationEventChannel preValidationEventChannel : preValidationEventChannels) {
                final String target = preValidationEventChannel.target();
                preValidationEventChannel.sources().stream()
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                        .entrySet().stream()
                        .filter(tc -> tc.getValue() > 1L)
                        .forEach(tc -> invalidDuplicates.add(
                                new InvalidSourceDuplicates(target, tc.getKey().functionalDomain(), tc.getKey().componentName(), tc.getValue())));
            }
            return invalidDuplicates;
        }
    }

    record PreValidationEventChannel(String target, List<PreValidationSource> sources) {

        PreValidationEventChannel {
            Objects.requireNonNull(target);
            Objects.requireNonNull(sources);
        }
    }

    record PreValidationSource(String functionalDomain, String componentName) {

        PreValidationSource {
            Objects.requireNonNull(functionalDomain);
            Objects.requireNonNull(componentName);
        }
    }

    @BuildStep
    List<ValidationErrorBuildItem> validate(final Capabilities capabilities,
                                            final CombinedIndexBuildItem combinedIndexBuildItem) {
        if (capabilities.isPresent(Capability.KAFKA)) {
            final IndexView computingIndex = combinedIndexBuildItem.getComputingIndex();
            final List<PreValidationEventChannel> preValidationEventChannels = new ArrayList<>();
            computingIndex.getAnnotations(EventChannel.class).forEach(eventChannel -> {
                final List<PreValidationSource> sources = new ArrayList<>();
                final String target = eventChannel.value("target").asString();
                eventChannel.value("sources").asArrayList().forEach(source -> {
                    final AnnotationInstance nested = source.asNested();
                    sources.add(new PreValidationSource(
                            nested.value("functionalDomain").asString(),
                            nested.value("componentName").asString()));
                });
                preValidationEventChannels.add(new PreValidationEventChannel(target, sources));
            });
            final PreValidation preValidation = new PreValidation(preValidationEventChannels);
            return Stream.concat(
                            preValidation.invalidNamings().stream(),
                            preValidation.invalidDuplicates().stream())
                    .map(invalidMessage ->
                            new ValidationErrorBuildItem(
                                    new IllegalArgumentException(invalidMessage.invalidMessage()))).toList();
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
    List<AdditionalBeanBuildItem> additionalBeans(final Capabilities capabilities,
                                                  final List<ValidationErrorBuildItem> validationErrorBuildItems) {
        if (capabilities.isPresent(Capability.KAFKA)) {
            return Stream.of(
                            EventChannel.class,
                            DebeziumConfigurator.class,
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
    void generateTargetsEventChannelConsumer(final List<TargetBuildItem> targetBuildItems,
                                             final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                             final BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemBuildProducer,
                                             final OutputTargetBuildItem outputTargetBuildItem) {
        targetBuildItems.stream()
                .flatMap(targetBuildItem -> targetBuildItem.sources().stream()
                        .map(src -> new TargetWithSource(targetBuildItem.target(), src))
                ).forEach(targetWithSource -> {
                    final String className = AbstractTargetEventChannelConsumer.class.getPackageName() + "."
                            + capitalize(targetWithSource.target().name())
                            + capitalize(targetWithSource.source().value())
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
                                                    Type.classType(JsonNodeEventRecord.class)
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
                                    MethodDescriptor.ofConstructor(ApplicationNaming.class, String.class, String.class),
                                    consume.load(targetWithSource.source().functionalDomain()),
                                    consume.load(targetWithSource.source().componentName()));
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
                                            ApplicationNaming.class,
                                            EventKey.class,
                                            EventRecord.class
                                    ),
                                    consume.getThis(),
                                    targetParam,
                                    applicationNamingParam,
                                    eventKeyParam,
                                    eventRecordParam
                            );
                            consume.returnValue(null);
                        }
                        CodeGenerationProcessor.writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
                    }
                });

        targetBuildItems.stream()
                .flatMap(targetBuildItem -> targetBuildItem.sources().stream()
                        .map(src -> new TargetWithSource(targetBuildItem.target(), src))
                ).forEach(targetWithSource -> {
                    final String channelNaming = targetWithSource.channel();
                    Map.of(
                                    "mp.messaging.incoming.%s.enable.auto.commit".formatted(channelNaming), "true",
                                    "mp.messaging.incoming.%s.auto.offset.reset".formatted(channelNaming), "earliest",
                                    "mp.messaging.incoming.%s.connector".formatted(channelNaming), "smallrye-kafka",
                                    "mp.messaging.incoming.%s.topic".formatted(channelNaming), channelNaming,
                                    "mp.messaging.incoming.%s.key.deserializer".formatted(channelNaming), JsonNodeEventKeyObjectMapperDeserializer.class.getName(),
                                    "mp.messaging.incoming.%s.value.deserializer".formatted(channelNaming), JsonNodeEventRecordObjectMapperDeserializer.class.getName(),
                                    "mp.messaging.incoming.%s.value.deserializer.key-type".formatted(channelNaming), EventKey.class.getName(),
                                    "mp.messaging.incoming.%s.value.deserializer.value-type".formatted(channelNaming), EventRecord.class.getName())
                            .forEach((key, value) -> runTimeConfigurationDefaultBuildItemBuildProducer.produce(new RunTimeConfigurationDefaultBuildItem(key, value)));
                });
    }

    private static String capitalize(final String input) {
        if (input == null || input.isBlank()) return input;
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
