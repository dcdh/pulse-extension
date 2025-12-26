package com.damdamdeo.pulse.extension.publisher.deployment.debezium;

import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.publisher.JsonNodeEventKey;
import com.damdamdeo.pulse.extension.publisher.JsonNodeEventValue;
import com.damdamdeo.pulse.extension.publisher.Consumer;
import com.damdamdeo.pulse.extension.publisher.Record;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class DebeziumPublisherTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Consumer consumer;

    @Inject
    OpenPGPEncryptionService openPGPEncryptionService;

    @Test
    void shouldConsumeFromKafkaTopic() {
        // Given
        final Timestamp givenCreationDate = Timestamp.from(Instant.ofEpochMilli(1_000_000_000L));
        final byte[] payload = openPGPEncryptionService.encrypt(
                // language=json
                """
                        {
                              "description": "lorem ipsum",
                              "status": "DONE",
                              "important": false
                            }
                        """.getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload();
        // language=sql
        final String sql = """
                INSERT INTO t_event (aggregate_root_id, aggregate_root_type, version, creation_date, event_type, event_payload, owned_by, belongs_to, executed_by) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement eventPreparedStatement = connection.prepareStatement(sql)) {
            eventPreparedStatement.setString(1, "Damien/0");
            eventPreparedStatement.setString(2, "Todo");
            eventPreparedStatement.setLong(3, 0);
            eventPreparedStatement.setObject(4, givenCreationDate);
            eventPreparedStatement.setString(5, "NewTodoCreated");
            eventPreparedStatement.setBytes(6, payload);
            eventPreparedStatement.setString(7, "Damien");
            eventPreparedStatement.setString(8, "Damien/0");
            eventPreparedStatement.setString(9, "EU:encodedbob");
            eventPreparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        // When
        final List<Record> records = consumer.consume("pulse.todotaking_todo.t_event");
        final Headers headers = records.getFirst().getHeaders();
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
                () -> assertThat(records.size()).isEqualTo(1L),
                () -> Assertions.assertThat(records.getFirst().getKey()).isEqualTo(new JsonNodeEventKey("Todo",
                        "Damien/0", 0)),
                () -> Assertions.assertThat(records.getFirst().getValue()).isEqualTo(new JsonNodeEventValue(1003_600_000_000L,// I do not understand the added part ...
                        "NewTodoCreated", payload, "Damien", "Damien/0", "EU:encodedbob")));
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
