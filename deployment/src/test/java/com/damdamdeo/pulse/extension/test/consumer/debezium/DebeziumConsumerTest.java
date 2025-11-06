package com.damdamdeo.pulse.extension.test.consumer.debezium;

import com.damdamdeo.pulse.extension.runtime.consumer.JsonNodeEventKey;
import com.damdamdeo.pulse.extension.runtime.consumer.JsonNodeEventValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.builder.Version;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerBuilder;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class DebeziumConsumerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-messaging-kafka", Version.getVersion())))
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    public static final class JsonNodeEventKeyObjectMapperDeserializer extends ObjectMapperDeserializer<JsonNodeEventKey> {

        public JsonNodeEventKeyObjectMapperDeserializer(final ObjectMapper objectMapper) {
            super(JsonNodeEventKey.class, objectMapper);
        }
    }

    public static final class JsonNodeEventRecordObjectMapperDeserializer extends ObjectMapperDeserializer<JsonNodeEventValue> {

        public JsonNodeEventRecordObjectMapperDeserializer(final ObjectMapper objectMapper) {
            super(JsonNodeEventValue.class, objectMapper);
        }
    }

    @Test
    void shouldConsumeFromKafkaTopic() {
        // Given
        final Timestamp givenCreationDate = Timestamp.from(Instant.ofEpochMilli(1_000_000_000L));
        // language=sql
        final String sql = """
                INSERT INTO t_event (aggregate_root_id, aggregate_root_type, version, creation_date, event_type, event_payload, owned_by) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement eventPreparedStatement = connection.prepareStatement(sql)) {
            eventPreparedStatement.setString(1, "Damien/0");
            eventPreparedStatement.setString(2, "com.damdamdeo.pulse.extension.core.Todo");
            eventPreparedStatement.setLong(3, 0);
            eventPreparedStatement.setObject(4, givenCreationDate);
            eventPreparedStatement.setString(5, "com.damdamdeo.pulse.extension.core.event.NewTodoCreated");
            eventPreparedStatement.setBytes(6,
                    // language=json
                    """
                            {
                              "id": "Damien/0",
                              "description": "lorem ipsum",
                              "status": "DONE",
                              "important": false
                            }
                            """.getBytes(StandardCharsets.UTF_8));
            eventPreparedStatement.setString(7, "Damien");
            eventPreparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        // When
        final ConsumerBuilder<JsonNodeEventKey, JsonNodeEventValue> consumer = new ConsumerBuilder<>(
                Map.of(
                        BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                        AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ProducerConfig.CLIENT_ID_CONFIG, "companion-" + UUID.randomUUID(),
                        ConsumerConfig.GROUP_ID_CONFIG, "my-group"),
                Duration.ofSeconds(10), new JsonNodeEventKeyObjectMapperDeserializer(objectMapper), new JsonNodeEventRecordObjectMapperDeserializer(objectMapper));
        final ConsumerTask<JsonNodeEventKey, JsonNodeEventValue> records = consumer.fromTopics("pulse.todotaking_todo.t_event", Duration.ofSeconds(10));

        // Then
        assertAll(
                () -> assertThat(records.awaitCompletion().count()).isEqualTo(1L),
                () -> assertThat(records.getFirstRecord().key()).isEqualTo(new JsonNodeEventKey("com.damdamdeo.pulse.extension.core.Todo",
                        "Damien/0", 0)),
                () -> assertThat(records.getFirstRecord().value()).isEqualTo(new JsonNodeEventValue(1003_600_000_000L,// I do not understand the added part ...
                        "com.damdamdeo.pulse.extension.core.event.NewTodoCreated",
                        // language=json
                        """
                                {
                                  "id": "Damien/0",
                                  "description": "lorem ipsum",
                                  "status": "DONE",
                                  "important": false
                                }
                                """.getBytes(StandardCharsets.UTF_8),
                        "Damien")));
    }
}
