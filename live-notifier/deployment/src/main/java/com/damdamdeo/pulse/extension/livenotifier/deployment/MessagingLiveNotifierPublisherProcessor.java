package com.damdamdeo.pulse.extension.livenotifier.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import com.damdamdeo.pulse.extension.common.deployment.items.EligibleTypeForSerializationBuildItem;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.livenotifier.deployment.items.EventBuildItem;
import com.damdamdeo.pulse.extension.livenotifier.runtime.LiveNotifierTopicNaming;
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
import org.apache.kafka.common.serialization.ByteArraySerializer;
import com.damdamdeo.pulse.extension.kafka.deployment.KafkaProcessor;

import java.util.List;
import java.util.Map;

import static com.damdamdeo.pulse.extension.common.deployment.CodeGenerationWriter.writeGeneratedClass;
import static org.apache.commons.lang3.StringUtils.capitalize;

public class MessagingLiveNotifierPublisherProcessor {

    @BuildStep
    List<ComposeServiceBuildItem> generateCompose() {
        return List.of(
                KafkaProcessor.KAFKA_COMPOSE_SERVICE_BUILD_ITEM);
    }

    @BuildStep
    void generateChannelPublisher(final ApplicationInfoBuildItem applicationInfoBuildItem,
                                  final BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemBuildProducer,
                                  final BuildProducer<ContentBuildItem> contentBuildItemBuildProducer) {
        final String topic = new LiveNotifierTopicNaming(new FromApplication(applicationInfoBuildItem.getName())).name();
        final Map<String, String> configurations = Map.of(
                "mp.messaging.outgoing.live-notification-out.group.id", applicationInfoBuildItem.getName(),
                "mp.messaging.outgoing.live-notification-out.enable.auto.commit", "true",
                "mp.messaging.outgoing.live-notification-out.auto.offset.reset", "latest",
                "mp.messaging.outgoing.live-notification-out.connector", "smallrye-kafka",
                "mp.messaging.outgoing.live-notification-out.topic", topic,
                "mp.messaging.outgoing.live-notification-out.value.serializer", ByteArraySerializer.class.getName());

        configurations.forEach((key, value) -> runTimeConfigurationDefaultBuildItemBuildProducer
                .produce(new RunTimeConfigurationDefaultBuildItem(key, value)));

        contentBuildItemBuildProducer.produce(new ContentBuildItem(new Title(Title.Level.SECOND, "Live Notifier configuration")));
        contentBuildItemBuildProducer.produce(new ContentBuildItem(CodeBlock.fromProperties(configurations)));
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
