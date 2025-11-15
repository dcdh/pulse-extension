package com.damdamdeo.pulse.extension.publisher.deployment.debezium;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerBuilder;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.assertj.core.api.Assertions;
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
import java.util.*;
import java.util.stream.Stream;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class DebeziumConsumerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    record JsonNodeEventKey(@JsonProperty("aggregate_root_type") String aggregateRootType,
                            @JsonProperty("aggregate_root_id") String aggregateRootId,
                            @JsonProperty("version") Integer version) {

        public JsonNodeEventKey {
            Objects.requireNonNull(aggregateRootType);
            Objects.requireNonNull(aggregateRootId);
            Objects.requireNonNull(version);
        }

        @Override
        public String toString() {
            return "JsonNodeEventKey{" +
                    "aggregateRootType='" + aggregateRootType + '\'' +
                    ", aggregateRootId='" + aggregateRootId + '\'' +
                    ", version=" + version +
                    '}';
        }
    }

    record JsonNodeEventValue(@JsonProperty("creation_date") Long createDate,
                              @JsonProperty("event_type") String eventType,
                              @JsonProperty("event_payload") byte[] eventPayload,
                              @JsonProperty("owned_by") String ownedBy,
                              @JsonProperty("belongs_to") String belongsTo) {

        public JsonNodeEventValue {
            Objects.requireNonNull(createDate);
            Objects.requireNonNull(eventType);
            Objects.requireNonNull(eventPayload);
            Objects.requireNonNull(ownedBy);
            Objects.requireNonNull(belongsTo);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            JsonNodeEventValue that = (JsonNodeEventValue) o;
            return Objects.equals(ownedBy, that.ownedBy)
                    && Objects.equals(createDate, that.createDate)
                    && Objects.equals(eventType, that.eventType)
                    && Arrays.equals(eventPayload, that.eventPayload)
                    && Objects.equals(belongsTo, that.belongsTo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(createDate, eventType, Arrays.hashCode(eventPayload), ownedBy, belongsTo);
        }

        @Override
        public String toString() {
            return "JsonNodeEventRecord{" +
                    "createDate=" + createDate +
                    ", eventType='" + eventType + '\'' +
                    ", eventPayload=" + Arrays.toString(eventPayload) +
                    ", ownedBy='" + ownedBy + '\'' +
                    ", belongsTo='" + belongsTo + '\'' +
                    '}';
        }
    }

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
                INSERT INTO t_event (aggregate_root_id, aggregate_root_type, version, creation_date, event_type, event_payload, owned_by, belongs_to) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement eventPreparedStatement = connection.prepareStatement(sql)) {
            eventPreparedStatement.setString(1, "Damien/0");
            eventPreparedStatement.setString(2, "Todo");
            eventPreparedStatement.setLong(3, 0);
            eventPreparedStatement.setObject(4, givenCreationDate);
            eventPreparedStatement.setString(5, "NewTodoCreated");
            eventPreparedStatement.setBytes(6,
                    // language=json
                    """
                            {
                              "description": "lorem ipsum",
                              "status": "DONE",
                              "important": false
                            }
                            """.getBytes(StandardCharsets.UTF_8));
            eventPreparedStatement.setString(7, "Damien");
            eventPreparedStatement.setString(8, "Damien/0");
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
        final ConsumerTask<JsonNodeEventKey, JsonNodeEventValue> records = consumer.fromTopics("pulse.todotaking_todo.t_event", Duration.ofSeconds(10)).awaitCompletion();
        records.close();
        final Headers headers = records.getFirstRecord().headers();
        final List<String> headersName = Stream.of(headers.toArray()).map(Header::key).toList();
        // Then
        assertAll(
                () -> assertThat(headersName).containsExactly(
                        "__debezium.context.connectorLogicalName",
                        "__debezium.context.taskId",
                        "__debezium.context.connectorName",
                        "__source_version",
                        "__source_connector",
                        "__source_name",
                        "__source_ts_ms",
                        "__source_db",
                        "__source_schema",
                        "__source_table",
                        "__source_txId",
                        "__source_lsn"),
                () -> assertThat(getValuesByKey(headers, "__debezium.context.connectorLogicalName")).containsExactly("pulse"),
                () -> assertThat(getValuesByKey(headers, "__debezium.context.taskId")).containsExactly("0"),
                () -> assertThat(getValuesByKey(headers, "__debezium.context.connectorName")).containsExactly("postgresql"),
                () -> assertThat(getValuesByKey(headers, "__source_version")).containsExactly("3.3.1.Final"),
                () -> assertThat(getValuesByKey(headers, "__source_connector")).containsExactly("postgresql"),
                () -> assertThat(getValuesByKey(headers, "__source_name")).containsExactly("pulse"),
                () -> assertThat(getValuesByKey(headers, "__source_ts_ms")).hasSize(1),
                () -> assertThat(getValuesByKey(headers, "__source_db")).containsExactly("quarkus"),
                () -> assertThat(getValuesByKey(headers, "__source_schema")).containsExactly("todotaking_todo"),
                () -> assertThat(getValuesByKey(headers, "__source_table")).containsExactly("t_event"),
                () -> assertThat(getValuesByKey(headers, "__source_txId")).hasSize(1),
                () -> assertThat(getValuesByKey(headers, "__source_lsn")).hasSize(1),
                () -> assertThat(records.count()).isEqualTo(1L),
                () -> Assertions.assertThat(records.getFirstRecord().key()).isEqualTo(new JsonNodeEventKey("Todo",
                        "Damien/0", 0)),
                () -> Assertions.assertThat(records.getFirstRecord().value()).isEqualTo(new JsonNodeEventValue(1003_600_000_000L,// I do not understand the added part ...
                        "NewTodoCreated",
                        // language=json
                        """
                                {
                                  "description": "lorem ipsum",
                                  "status": "DONE",
                                  "important": false
                                }
                                """.getBytes(StandardCharsets.UTF_8),
                        "Damien", "Damien/0")));
    }

    private static List<String> getValuesByKey(final Headers headers, final String key) {
        final List<String> values = new ArrayList<>();
        final Iterator<Header> iterator = headers.headers(key).iterator();
        while (iterator.hasNext()) {
            final Header header = iterator.next();
            values.add(new String(header.value()));
        }
        return values;
    }

}
