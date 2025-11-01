package com.damdamdeo.pulse.extension.test.consumer;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.InRelationWith;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.event.TodoMarkedAsDone;
import com.damdamdeo.pulse.extension.runtime.consumer.EventChannel;
import com.damdamdeo.pulse.extension.runtime.consumer.JsonNodeEventKey;
import com.damdamdeo.pulse.extension.runtime.consumer.JsonNodeEventRecord;
import com.damdamdeo.pulse.extension.runtime.encryption.OpenPGPEncryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.builder.Version;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.reactive.messaging.kafka.companion.ProducerBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class TargetEventChannelConsumerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .overrideConfigKey("pulse.debezium.enabled", "false")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-messaging-kafka", Version.getVersion())))
            .withConfigurationResource("application.properties");

    @ApplicationScoped
    static class StubPassphraseRepository implements PassphraseRepository {

        @Override
        public Optional<Passphrase> retrieve(OwnedBy ownedBy) {
            return Optional.of(PassphraseSample.PASSPHRASE);
        }

        @Override
        public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
            throw new IllegalStateException("Should not be calld !");
        }
    }

    record Call(Target target,
                AggregateRootType aggregateRootType,
                AggregateId aggregateId,
                CurrentVersionInConsumption currentVersionInConsumption,
                Instant creationDate,
                EventType eventType,
                EncryptedPayload encryptedPayload,
                OwnedBy ownedBy,
                JsonNode decryptedEventPayload,
                AggregateRootLoaded<JsonNode> aggregateRootLoaded) {
        Call {
            Objects.requireNonNull(target);
            Objects.requireNonNull(aggregateRootType);
            Objects.requireNonNull(aggregateId);
            Objects.requireNonNull(currentVersionInConsumption);
            Objects.requireNonNull(creationDate);
            Objects.requireNonNull(eventType);
            Objects.requireNonNull(encryptedPayload);
            Objects.requireNonNull(ownedBy);
            Objects.requireNonNull(decryptedEventPayload);
            Objects.requireNonNull(aggregateRootLoaded);
        }
    }

    @ApplicationScoped
    @EventChannel(target = "statistics",
            sources = {
                    @EventChannel.Source(functionalDomain = "TodoTaking", componentName = "Todo")
            })
    static final class StatisticsEventHandler implements AsyncEventChannelMessageHandler<JsonNode> {

        private Call call = null;

        @Override
        public void handleMessage(final Target target,
                                  final AggregateRootType aggregateRootType,
                                  final AggregateId aggregateId,
                                  final CurrentVersionInConsumption currentVersionInConsumption,
                                  final Instant creationDate,
                                  final EventType eventType,
                                  final EncryptedPayload encryptedPayload,
                                  final OwnedBy ownedBy,
                                  final JsonNode decryptedEventPayload,
                                  final Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier) {
            this.call = new Call(
                    target, aggregateRootType, aggregateId, currentVersionInConsumption, creationDate, eventType,
                    encryptedPayload, ownedBy, decryptedEventPayload,
                    aggregateRootLoadedSupplier.get());
        }

        public Call getCall() {
            return call;
        }
    }

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @Any
    Instance<StatisticsEventHandler> statisticsEventHandlerInstance;

    public static final class JsonNodeEventKeyObjectMapperSerializer extends ObjectMapperSerializer<JsonNodeEventKey> {

    }

    public static final class JsonNodeEventRecordObjectMapperSerializer extends ObjectMapperSerializer<JsonNodeEventRecord> {

    }

    private static final String TOPIC = "statistics-todotaking-todo";

    @Test
    void shouldConsumeEvent() throws SQLException {
        final StatisticsEventHandler statisticsEventHandler = statisticsEventHandlerInstance.select(
                EventChannel.Literal.of("statistics")).get();
        // from PostgresAggregateRootLoaderTest#shouldReturnAggregate
        // Given
        // language=json
        final String aggregatePayload = """
                {
                  "id": "Damien/0",
                  "description": "lorem ipsum",
                  "status": "DONE",
                  "important": false
                }
                """;
        final byte[] encryptedAggregatePayload = OpenPGPEncryptionService.encrypt(aggregatePayload.getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload();
        // language=sql
        final String aggregateRootSql = """
                    INSERT INTO t_aggregate_root (aggregate_root_type, aggregate_root_id, last_version, aggregate_root_payload, owned_by, in_relation_with)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(aggregateRootSql)) {
            ps.setString(1, Todo.class.getName());
            ps.setString(2, "Damien/0");
            ps.setLong(3, 1);
            ps.setBytes(4, encryptedAggregatePayload);
            ps.setString(5, "Damien");
            ps.setString(6, "Damien/0");
            ps.executeUpdate();
        }

        // language=sql
        final String idempotencySql = """
                INSERT INTO t_idempotency (target, source, aggregate_root_type, aggregate_root_id, last_consumed_version)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(idempotencySql)) {
            ps.setString(1, "statistics");
            ps.setString(2, new ApplicationNaming("TodoTaking", "Todo").value());
            ps.setString(3, Todo.class.getName());
            ps.setString(4, "Damien/0");
            ps.setLong(5, 0);
            ps.executeUpdate();
        }

        // language=json
        final String todoMarkedAsDonePayload = """
                {
                  "id": "Damien/0"
                }
                """;
        final byte[] encryptedTodoMarkedAsDonePayload = OpenPGPEncryptionService.encrypt(todoMarkedAsDonePayload.getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload();

        // When
        new ProducerBuilder<>(
                Map.of(
                        BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                        ProducerConfig.CLIENT_ID_CONFIG, "companion-" + UUID.randomUUID()),
                Duration.ofSeconds(10), new JsonNodeEventKeyObjectMapperSerializer(), new JsonNodeEventRecordObjectMapperSerializer())
                .usingGenerator(
                        integer -> new ProducerRecord<>(TOPIC,
                                new JsonNodeEventKey(Todo.class.getName(), "Damien/0", 1),
                                new JsonNodeEventRecord(Todo.class.getName(), "Damien/0", 1, 1_761_335_312_527L,
                                        TodoMarkedAsDone.class.getName(),
                                        encryptedTodoMarkedAsDonePayload,
                                        "Damien")), 1L);

        // Then
        await().atMost(10, TimeUnit.SECONDS).until(() -> statisticsEventHandler.getCall() != null);
        final ObjectNode expectedTodoMarkedAsDonePayload = objectMapper.createObjectNode();
        expectedTodoMarkedAsDonePayload.put("id", "Damien/0");
        final ObjectNode expectedAggregateRootPayload = objectMapper.createObjectNode();
        expectedAggregateRootPayload.put("id", "Damien/0");
        expectedAggregateRootPayload.put("description", "lorem ipsum");
        expectedAggregateRootPayload.put("status", "DONE");
        expectedAggregateRootPayload.put("important", false);
        assertThat(statisticsEventHandler.getCall()).isEqualTo(
                new Call(
                        new Target("statistics"),
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Damien/0"),
                        new CurrentVersionInConsumption(1),
                        Instant.ofEpochMilli(1_761_335_312_527L),
                        EventType.from(TodoMarkedAsDone.class),
                        new EncryptedPayload(encryptedTodoMarkedAsDonePayload),
                        new OwnedBy("Damien"),
                        expectedTodoMarkedAsDonePayload,
                        new AggregateRootLoaded<>(
                                AggregateRootType.from(Todo.class),
                                new AnyAggregateId("Damien/0"),
                                new LastAggregateVersion(1),
                                new EncryptedPayload(encryptedAggregatePayload),
                                expectedAggregateRootPayload,
                                new OwnedBy("Damien"),
                                new InRelationWith("Damien/0"))));
    }
}
