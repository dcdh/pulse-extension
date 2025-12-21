package com.damdamdeo.pulse.extension.livenotifier.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.EligibleTypeForSerializationBuildItem;
import com.damdamdeo.pulse.extension.livenotifier.deployment.items.EventBuildItem;
import com.damdamdeo.pulse.extension.livenotifier.runtime.DefaultObjectMapperSerializer;
import com.damdamdeo.pulse.extension.livenotifier.runtime.MessagingLiveNotifierPublisher;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.gizmo.Type;
import jakarta.inject.Singleton;

import java.util.List;

import static com.damdamdeo.pulse.extension.common.deployment.CodeGenerationWriter.writeGeneratedClass;
import static org.apache.commons.lang3.StringUtils.capitalize;

public class MessagingLiveNotifierPublisherProcessor {

    @BuildStep
    List<RunTimeConfigurationDefaultBuildItem> generateChannelPublisher(final ApplicationInfoBuildItem applicationInfoBuildItem) {
        final String topic = "pulse.live-notification.%s".formatted(applicationInfoBuildItem.getName().toLowerCase());
        return List.of(
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.outgoing.live-notification-out.group.id", applicationInfoBuildItem.getName()),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.outgoing.live-notification-out.enable.auto.commit", "true"),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.outgoing.live-notification-out.auto.offset.reset", "latest"),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.outgoing.live-notification-out.connector", "smallrye-kafka"),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.outgoing.live-notification-out.topic", topic),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.outgoing.live-notification-out.value.serializer", DefaultObjectMapperSerializer.class.getName()),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.outgoing.live-notification-out.value.serializer.value-type", Object.class.getName())
        );
    }

    @BuildStep
    public List<EligibleTypeForSerializationBuildItem> eligibleTypeForSerializationBuildItemsToSerialize(
            final List<EventBuildItem> eventBuildItems) {
        return eventBuildItems.stream()
                .map(EventBuildItem::getEventClazz)
                .map(EligibleTypeForSerializationBuildItem::new)
                .toList();
    }

    @BuildStep
    void generatePublisherBeans(final List<EventBuildItem> eventBuildItems,
                                final BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
                                final OutputTargetBuildItem outputTargetBuildItem) {
        eventBuildItems.stream()
                .map(EventBuildItem::getEventClazz)
                .forEach(eventClazz -> {
                    try (final ClassCreator beanClassCreator = ClassCreator.builder()
                            .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer))
                            .className(capitalize(eventClazz.getSimpleName()).replaceAll("\\$", "_") + "MessagingLiveNotifierPublisher")
                            .signature(SignatureBuilder.forClass()
                                    .setSuperClass(
                                            Type.parameterizedType(
                                                    Type.classType(MessagingLiveNotifierPublisher.class),
                                                    Type.classType(eventClazz))))
                            .setFinal(true)
                            .build()) {
                        beanClassCreator.addAnnotation(Singleton.class);
                        beanClassCreator.addAnnotation(Unremovable.class);
                        beanClassCreator.addAnnotation(DefaultBean.class);

                        writeGeneratedClass(beanClassCreator, outputTargetBuildItem);
                    }
                });
    }
}
