package com.damdamdeo.pulse.extension.livenotifier.deployment.consumer;

import com.damdamdeo.pulse.extension.livenotifier.deployment.items.EventBuildItem;
import com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.JacksonHeaderBasedDeserializer;
import com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.MessagingConsumer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.*;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.damdamdeo.pulse.extension.common.deployment.CodeGenerationWriter.writeGeneratedClass;
import static io.quarkus.gizmo.Type.parameterizedType;

public class LiveConsumerProcessor {

    public static final String GENERATED_JACKSON_HEADER_BASED_DESERIALIZER_NAME = JacksonHeaderBasedDeserializer.class.getName() + "Generated";

    @BuildStep
    List<RunTimeConfigurationDefaultBuildItem> generateChannelConsumer(final ApplicationInfoBuildItem applicationInfoBuildItem) {
        final String topic = "pulse.live-notification.%s".formatted(applicationInfoBuildItem.getName().toLowerCase());
        return List.of(
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.incoming.live-notification-in.group.id", "%s_%s".formatted(applicationInfoBuildItem.getName(), UUID.randomUUID())),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.incoming.live-notification-in.enable.auto.commit", "true"),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.incoming.live-notification-in.auto.offset.reset", "latest"),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.incoming.live-notification-in.connector", "smallrye-kafka"),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.incoming.live-notification-in.topic", topic),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.incoming.live-notification-in.value.deserializer", GENERATED_JACKSON_HEADER_BASED_DESERIALIZER_NAME)
        );
    }

    @BuildStep
    AdditionalBeanBuildItem produceAdditionalBeanBuildItem() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(MessagingConsumer.class)
                .setUnremovable().build();
    }

    @BuildStep
    void generateJacksonHeaderBasedDeserializer(final List<EventBuildItem> eventBuildItems,
                                                final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                                final OutputTargetBuildItem outputTargetBuildItem) {
        try (final ClassCreator beanClassCreator = ClassCreator.builder()
                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                .className(GENERATED_JACKSON_HEADER_BASED_DESERIALIZER_NAME)
                .signature(SignatureBuilder.forClass()
                        .setSuperClass(Type.classType(JacksonHeaderBasedDeserializer.class)))
                .setFinal(true)
                .build()) {

            final MethodCreator mixinsMethod = beanClassCreator.getMethodCreator("mixins", Map.class);
            mixinsMethod.setSignature(
                    SignatureBuilder.forMethod()
                            .setReturnType(parameterizedType(
                                    io.quarkus.gizmo.Type.classType(Map.class),
                                    io.quarkus.gizmo.Type.classType(String.class),
                                    io.quarkus.gizmo.Type.classType(String.class)))
                            .build());
            mixinsMethod.setModifiers(Modifier.PROTECTED);

            final ResultHandle map = mixinsMethod.newInstance(
                    MethodDescriptor.ofConstructor(HashMap.class)
            );

            for (final EventBuildItem discovered : eventBuildItems) {
                final String mixinClassName = discovered.getEventClazz().getName() + "Mixin";

                final ResultHandle sourceClass = mixinsMethod.load(discovered.getEventClazz().getName());
                final ResultHandle mixinClass = mixinsMethod.load(mixinClassName);

                mixinsMethod.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(HashMap.class, "put", Object.class, Object.class, Object.class),
                        map, sourceClass, mixinClass
                );
            }
            mixinsMethod.returnValue(map);

            writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
        }
    }
}
