package com.damdamdeo.pulse.extension.livenotifier.deployment.consumer;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.livenotifier.runtime.LiveNotifierTopicNaming;
import com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.MessagingConsumer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.util.Map;
import java.util.UUID;

public class LiveConsumerProcessor {

    @BuildStep
    void generateChannelConsumer(final ApplicationInfoBuildItem applicationInfoBuildItem,
                                 final BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemBuildProducer,
                                 final BuildProducer<ContentBuildItem> contentBuildItemBuildProducer) {
        final String topic = new LiveNotifierTopicNaming(FromApplication.from(applicationInfoBuildItem.getName())).name();
        final Map<String, String> configurations = Map.of(
                "mp.messaging.incoming.live-notification-in.group.id", "%s_%s".formatted(applicationInfoBuildItem.getName(), UUID.randomUUID()),
                "mp.messaging.incoming.live-notification-in.enable.auto.commit", "true",
                "mp.messaging.incoming.live-notification-in.auto.offset.reset", "latest",
                "mp.messaging.incoming.live-notification-in.connector", "smallrye-kafka",
                "mp.messaging.incoming.live-notification-in.topic", topic,
                "mp.messaging.incoming.live-notification-in.value.deserializer", ByteArrayDeserializer.class.getName());

        configurations.forEach((key, value) -> runTimeConfigurationDefaultBuildItemBuildProducer
                .produce(new RunTimeConfigurationDefaultBuildItem(key, value)));

        contentBuildItemBuildProducer.produce(new ContentBuildItem(new Title(2, "Live Consumer configuration")));
        contentBuildItemBuildProducer.produce(new ContentBuildItem(CodeBlock.fromProperties(configurations)));
    }

    @BuildStep
    AdditionalBeanBuildItem produceAdditionalBeanBuildItem() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(MessagingConsumer.class)
                .setUnremovable().build();
    }
}
