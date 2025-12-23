package com.damdamdeo.pulse.extension.livenotifier.deployment.consumer;

import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.livenotifier.SseConsumer;
import com.damdamdeo.pulse.extension.livenotifier.deployment.AbstractMessagingTest;
import com.damdamdeo.pulse.extension.livenotifier.runtime.LiveNotifierPublisher;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class LiveNotifierConsumerTest extends AbstractMessagingTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClass(StubPassphraseRepository.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false");

    @Inject
    LiveNotifierPublisher<NewTodoCreated> messagingLiveNotifierPublisher;

    @Inject
    SseConsumer sseConsumer;

    @Test
    void shouldConsumeNotificationUsingSse() throws Exception {
        // Given
        final CompletableFuture<List<String>> receivedEvents = sseConsumer.consume(null, Duration.ofSeconds(10));

        // When
        messagingLiveNotifierPublisher.publish("TodoEvents", new NewTodoCreated("another lorem ipsum"));

        // Then
        final List<String> ssePayload = receivedEvents.get(12, TimeUnit.SECONDS);

        assertThat(ssePayload).containsExactly(
                """
                        content-type:application/json
                        event:TodoEvents
                        data:{"description":"another lorem ipsum"}
                        """);
    }

    @Test
    void shouldGenerateMessagingConfiguration() {
        assertAll(
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.live-notification-in.group.id", String.class))
                        .startsWith("TodoTaking_Todo_"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.live-notification-in.enable.auto.commit", String.class))
                        .isEqualTo("true"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.live-notification-in.auto.offset.reset", String.class))
                        .isEqualTo("latest"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.live-notification-in.connector", String.class))
                        .isEqualTo("smallrye-kafka"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.live-notification-in.topic", String.class))
                        .isEqualTo("pulse.live-notification.todotaking_todo"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.live-notification-in.value.deserializer", String.class))
                        .isEqualTo("org.apache.kafka.common.serialization.ByteArrayDeserializer")
        );
    }
}
