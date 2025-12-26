package com.damdamdeo.pulse.extension.publisher;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerBuilder;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;

@ApplicationScoped
public class Consumer {

    @ConfigProperty(name = "quarkus.application.name")
    String quarkusApplicationName;

    public List<Record<JsonNodeEventKey, JsonNodeEventValue>> consumeFromTEvent() {
        final String topic = "pulse.%s.t_event".formatted(quarkusApplicationName.toLowerCase());
        try (final ConsumerBuilder<JsonNodeEventKey, JsonNodeEventValue> consumer = new ConsumerBuilder<>(
                Map.of(
                        BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                        AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ProducerConfig.CLIENT_ID_CONFIG, "companion-" + UUID.randomUUID(),
                        ConsumerConfig.GROUP_ID_CONFIG, "my-group"),
                Duration.ofSeconds(10),
                new ObjectMapperDeserializer<>(JsonNodeEventKey.class),
                new ObjectMapperDeserializer<>(JsonNodeEventValue.class));
             final ConsumerTask<JsonNodeEventKey, JsonNodeEventValue> records = consumer.fromTopics(topic, Duration.ofSeconds(10)).awaitCompletion()) {
            return records.stream().map(record ->
                            new Record<>(record.headers(), record.key(), record.value()))
                    .toList();
        }
    }

    public List<Record<JsonNodeAggregateRootKey, JsonNodeAggregateRootValue>> consumeFromTAggregateRoot() {
        final String topic = "pulse.%s.t_aggregate_root".formatted(quarkusApplicationName.toLowerCase());
        try (final ConsumerBuilder<JsonNodeAggregateRootKey, JsonNodeAggregateRootValue> consumer = new ConsumerBuilder<>(
                Map.of(
                        BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                        AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ProducerConfig.CLIENT_ID_CONFIG, "companion-" + UUID.randomUUID(),
                        ConsumerConfig.GROUP_ID_CONFIG, "my-group"),
                Duration.ofSeconds(10),
                new ObjectMapperDeserializer<>(JsonNodeAggregateRootKey.class),
                new ObjectMapperDeserializer<>(JsonNodeAggregateRootValue.class));
             final ConsumerTask<JsonNodeAggregateRootKey, JsonNodeAggregateRootValue> records = consumer.fromTopics(topic, Duration.ofSeconds(10)).awaitCompletion()) {
            return records.stream().map(record ->
                            new Record<>(record.headers(), record.key(), record.value()))
                    .toList();
        }
    }
}
