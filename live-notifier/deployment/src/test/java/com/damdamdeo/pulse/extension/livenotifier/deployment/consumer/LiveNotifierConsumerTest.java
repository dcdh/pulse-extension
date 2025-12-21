package com.damdamdeo.pulse.extension.livenotifier.deployment.consumer;

import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.livenotifier.runtime.LiveNotifierPublisher;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class LiveNotifierConsumerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @Inject
    LiveNotifierPublisher<NewTodoCreated> messagingLiveNotifierPublisher;

    @Test
    void shouldConsumeNotificationUsingSse() throws Exception {
        // Given
        final Integer testPort = ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final CompletableFuture<String> receivedEvent = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                final URL url = new URI("http://localhost:%d/notifier/sse/stream".formatted(testPort)).toURL();
                final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.connect();

                try (final BufferedReader reader =
                             new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

                    String line;
                    StringBuilder event = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) {
                            receivedEvent.complete(event.toString());
                            break;
                        }
                        event.append(line).append("\n");
                    }
                }
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        Thread.sleep(1000);

        // When
        messagingLiveNotifierPublisher.publish("TodoEvents", new NewTodoCreated("another lorem ipsum"));

        // Then
        final String ssePayload = receivedEvent.get(10, TimeUnit.SECONDS);

        assertThat(ssePayload).isEqualTo(
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
                        .isEqualTo("com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.JacksonHeaderBasedDeserializerGenerated")
        );
    }
}
