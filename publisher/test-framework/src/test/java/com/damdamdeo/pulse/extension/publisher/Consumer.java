package com.damdamdeo.pulse.extension.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerBuilder;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.eclipse.microprofile.config.ConfigProvider;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;

@ApplicationScoped
public class Consumer {

    @Inject
    ObjectMapper objectMapper;

    public List<Record> consume(final String topic) {
        final ConsumerBuilder<JsonNodeEventKey, JsonNodeEventValue> consumer = new ConsumerBuilder<>(
                Map.of(
                        BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                        AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ProducerConfig.CLIENT_ID_CONFIG, "companion-" + UUID.randomUUID(),
                        ConsumerConfig.GROUP_ID_CONFIG, "my-group"),
                Duration.ofSeconds(10), new JsonNodeEventKeyObjectMapperDeserializer(objectMapper), new JsonNodeEventRecordObjectMapperDeserializer(objectMapper));
        final ConsumerTask<JsonNodeEventKey, JsonNodeEventValue> records = consumer.fromTopics(topic, Duration.ofSeconds(10)).awaitCompletion();
        records.close();
        return records.stream().map(record ->
                new Record(record.headers(), record.key(), record.value())).toList();
    }
}
