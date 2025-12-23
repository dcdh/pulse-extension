package com.damdamdeo.pulse.extension.livenotifier.deployment;

import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.livenotifier.Consumer;
import com.damdamdeo.pulse.extension.livenotifier.Record;
import com.damdamdeo.pulse.extension.livenotifier.runtime.LiveNotifierPublisher;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class MessagingLiveNotifierPublisherTest extends AbstractMessagingTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClass(StubPassphraseRepository.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false");

    @Inject
    Consumer consumer;

    @Inject
    LiveNotifierPublisher<NewTodoCreated> messagingLiveNotifierPublisher;

    @Test
    void shouldConsumeRecordFromLiveNotificationKafka() {
        // Given

        // When
        messagingLiveNotifierPublisher.publish("TodoEvents", new NewTodoCreated("lorem ipsum"));

        // Then
        final List<Record> records = consumer.consume("pulse.live-notification.todotaking_todo");
        final Headers headers = records.getFirst().getHeaders();
        final List<String> headersName = Stream.of(headers.toArray()).map(Header::key).toList();

        assertAll(
                () -> assertThat(headersName).containsExactly("event-name", "content-type", "owned-by"),
                () -> assertThat(getValuesByKey(headers, "event-name")).containsExactly("TodoEvents"),
                () -> assertThat(getValuesByKey(headers, "content-type")).containsExactly("application/vnd.com.damdamdeo.pulse.extension.core.event.NewTodoCreated.api+json"),
                () -> assertThat(getValuesByKey(headers, "owned-by")).isEmpty(),
                () -> assertThat(records.getFirst().getKey()).isNull(),
                () -> JSONAssert.assertEquals(
                        // language=json
                        """
                                {
                                  "description" : "lorem ipsum"
                                }
                                """, new String(records.getFirst().getValue()), JSONCompareMode.STRICT));
    }

    @Test
    void shouldConsumeEncryptedRecordFromLiveNotificationKafka() {
        // Given

        // When
        messagingLiveNotifierPublisher.publish("TodoEvents", new NewTodoCreated("lorem ipsum"), new OwnedBy("alice"));

        // Then
        final List<Record> records = consumer.consume("pulse.live-notification.todotaking_todo");
        final Headers headers = records.getFirst().getHeaders();
        final List<String> headersName = Stream.of(headers.toArray()).map(Header::key).toList();

        assertAll(
                () -> assertThat(getValuesByKey(headers, "owned-by")).containsExactly("alice"),
                () -> assertThat(records.getFirst().getValue()).startsWith(-61, 30)
        );
    }

    @Test
    void shouldGenerateMessagingConfiguration() {
        assertAll(
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.outgoing.live-notification-out.group.id", String.class))
                        .isEqualTo("TodoTaking_Todo"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.outgoing.live-notification-out.enable.auto.commit", String.class))
                        .isEqualTo("true"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.outgoing.live-notification-out.auto.offset.reset", String.class))
                        .isEqualTo("latest"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.outgoing.live-notification-out.connector", String.class))
                        .isEqualTo("smallrye-kafka"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.outgoing.live-notification-out.topic", String.class))
                        .isEqualTo("pulse.live-notification.todotaking_todo"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.outgoing.live-notification-out.value.serializer", String.class))
                        .isEqualTo("org.apache.kafka.common.serialization.ByteArraySerializer")
        );
    }

    public static List<String> getValuesByKey(final Headers headers, final String key) {
        final List<String> values = new ArrayList<>();
        final Iterator<Header> iterator = headers.headers(key).iterator();
        while (iterator.hasNext()) {
            final Header header = iterator.next();
            if (header.value() != null) {
                values.add(new String(header.value()));
            }
        }
        return values;
    }
}
