package com.damdamdeo.pulse.extension.livenotifier.deployment.consumer;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.livenotifier.runtime.LiveNotifierTopicNaming;
import com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.MessagingConsumer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.util.List;
import java.util.UUID;

public class LiveConsumerProcessor {

    @BuildStep
    List<RunTimeConfigurationDefaultBuildItem> generateChannelConsumer(final ApplicationInfoBuildItem applicationInfoBuildItem) {
        final String topic = new LiveNotifierTopicNaming(FromApplication.from(applicationInfoBuildItem.getName())).name();
        return List.of(
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.incoming.live-notification-in.group.id", "%s_%s".formatted(applicationInfoBuildItem.getName(), UUID.randomUUID())),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.incoming.live-notification-in.enable.auto.commit", "true"),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.incoming.live-notification-in.auto.offset.reset", "latest"),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.incoming.live-notification-in.connector", "smallrye-kafka"),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.incoming.live-notification-in.topic", topic),
                new RunTimeConfigurationDefaultBuildItem("mp.messaging.incoming.live-notification-in.value.deserializer", ByteArrayDeserializer.class.getName())
        );
    }

    @BuildStep
    AdditionalBeanBuildItem produceAdditionalBeanBuildItem() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(MessagingConsumer.class)
                .setUnremovable().build();
    }
}
