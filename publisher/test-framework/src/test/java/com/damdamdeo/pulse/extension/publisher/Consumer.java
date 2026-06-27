package com.damdamdeo.pulse.extension.publisher;

import com.damdamdeo.pulse.extension.core.consumer.CdcTopicNaming;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Table;
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
import java.util.Objects;
import java.util.UUID;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;

@ApplicationScoped
public class Consumer {

    @ConfigProperty(name = "quarkus.application.name")
    String quarkusApplicationName;

    private enum Target {
        EVENT {
            @Override
            Table table() {
                return Table.EVENT;
            }
        },
        AGGREGATE_ROOT {
            @Override
            Table table() {
                return Table.AGGREGATE_ROOT;
            }
        };

        abstract Table table();
    }

    public List<Record<JsonNodeEventKey, JsonNodeEventValue>> consumeFromTEvent() {
        return consumeFrom(Target.EVENT, JsonNodeEventKey.class, JsonNodeEventValue.class);
    }

    public List<Record<JsonNodeAggregateRootKey, JsonNodeAggregateRootValue>> consumeFromTAggregateRoot() {
        return consumeFrom(Target.AGGREGATE_ROOT, JsonNodeAggregateRootKey.class, JsonNodeAggregateRootValue.class);
    }

    private <K, V> List<Record<K, V>> consumeFrom(final Target target, final Class<K> keyClass, final Class<V> valueClass) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(keyClass);
        Objects.requireNonNull(valueClass);
        final String topic = CdcTopicNaming.from(new FromApplication(quarkusApplicationName), target.table()).name();
        try (final ConsumerBuilder<K, V> consumer = new ConsumerBuilder<>(
                Map.of(
                        BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                        AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ProducerConfig.CLIENT_ID_CONFIG, "companion-" + UUID.randomUUID(),
                        ConsumerConfig.GROUP_ID_CONFIG, "my-group"),
                Duration.ofSeconds(10),
                new ObjectMapperDeserializer<>(keyClass),
                new ObjectMapperDeserializer<>(valueClass));
             final ConsumerTask<K, V> records = consumer.fromTopics(topic, Duration.ofSeconds(10)).awaitCompletion(Duration.ofSeconds(60))) {
            return records.stream().map(record ->
                            new Record<>(record.headers(), record.key(), record.value()))
                    .toList();
        }
    }
}
