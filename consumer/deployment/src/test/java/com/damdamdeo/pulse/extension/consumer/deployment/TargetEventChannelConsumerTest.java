package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.consumer.runtime.EventChannel;
import com.damdamdeo.pulse.extension.consumer.runtime.JsonNodeEventKey;
import com.damdamdeo.pulse.extension.consumer.runtime.JsonNodeEventValue;
import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.event.TodoMarkedAsDone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.reactive.messaging.kafka.companion.ProducerBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class TargetEventChannelConsumerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClass(StatisticsEventHandler.class))
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .withConfigurationResource("application.properties");

    @ApplicationScoped
    static class StubPassphraseRepository implements PassphraseRepository {

        @Override
        public Optional<Passphrase> retrieve(final OwnedBy ownedBy) {
            if (new OwnedBy("Damien").equals(ownedBy)) {
                return Optional.of(PassphraseSample.PASSPHRASE);
            } else {
                return Optional.empty();
            }
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
                DecryptablePayload<JsonNode> decryptableEventPayload,
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
            Objects.requireNonNull(decryptableEventPayload);
            Objects.requireNonNull(aggregateRootLoaded);
        }
    }

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    OpenPGPEncryptionService openPGPEncryptionService;

    @Inject
    @Any
    Instance<StatisticsEventHandler> statisticsEventHandlerInstance;

    public static final class JsonNodeEventKeyObjectMapperSerializer extends ObjectMapperSerializer<JsonNodeEventKey> {

    }

    public static final class JsonNodeEventRecordObjectMapperSerializer extends ObjectMapperSerializer<JsonNodeEventValue> {

    }

    private static final String TOPIC = "pulse.todotaking_todo.t_event";

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
        final byte[] encryptedAggregatePayload = openPGPEncryptionService.encrypt(aggregatePayload.getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload();
        // language=sql
        final String aggregateRootSql = """
                    INSERT INTO todotaking_todo.t_aggregate_root (aggregate_root_type, aggregate_root_id, last_version, aggregate_root_payload, owned_by, belongs_to)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(aggregateRootSql)) {
            ps.setString(1, Todo.class.getSimpleName());
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
            ps.setString(3, Todo.class.getSimpleName());
            ps.setString(4, "Damien/0");
            ps.setLong(5, 0);
            ps.executeUpdate();
        }

        // language=json
        final String todoMarkedAsDonePayload = """
                {}
                """;
        final byte[] encryptedTodoMarkedAsDonePayload = openPGPEncryptionService.encrypt(todoMarkedAsDonePayload.getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload();

        // When
        new ProducerBuilder<>(
                Map.of(
                        BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                        AUTO_OFFSET_RESET_CONFIG, "latest",
                        ProducerConfig.CLIENT_ID_CONFIG, "companion-" + UUID.randomUUID()),
                Duration.ofSeconds(10), new JsonNodeEventKeyObjectMapperSerializer(), new JsonNodeEventRecordObjectMapperSerializer())
                .usingGenerator(
                        integer -> new ProducerRecord<>(TOPIC,
                                new JsonNodeEventKey(Todo.class.getSimpleName(), "Damien/0", 1),
                                new JsonNodeEventValue(1_761_335_312_527L,
                                        TodoMarkedAsDone.class.getSimpleName(),
                                        encryptedTodoMarkedAsDonePayload,
                                        "Damien", "Damien/0")), 1L);

        // Then
        await().atMost(10, TimeUnit.SECONDS).until(() -> statisticsEventHandler.getCall() != null);
        final ObjectNode expectedTodoMarkedAsDonePayload = objectMapper.createObjectNode();
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
                        DecryptablePayload.ofDecrypted(expectedTodoMarkedAsDonePayload),
                        new AggregateRootLoaded<>(
                                AggregateRootType.from(Todo.class),
                                new AnyAggregateId("Damien/0"),
                                new LastAggregateVersion(1),
                                new EncryptedPayload(encryptedAggregatePayload),
                                DecryptablePayload.ofDecrypted(expectedAggregateRootPayload),
                                new OwnedBy("Damien"),
                                new BelongsTo(new AnyAggregateId("Damien/0")))));
    }

    @Test
    void shouldConsumeEventWhenPassPhraseDoesNotExistsAnymore() throws SQLException {
        final StatisticsEventHandler statisticsEventHandler = statisticsEventHandlerInstance.select(
                EventChannel.Literal.of("statistics")).get();
        // from PostgresAggregateRootLoaderTest#shouldReturnAggregate
        // Given
        // language=json
        final String aggregatePayload = """
                {
                  "id": "Alban/0",
                  "description": "lorem ipsum",
                  "status": "DONE",
                  "important": false
                }
                """;
        final byte[] encryptedAggregatePayload = openPGPEncryptionService.encrypt(aggregatePayload.getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload();
        // language=sql
        final String aggregateRootSql = """
                    INSERT INTO todotaking_todo.t_aggregate_root (aggregate_root_type, aggregate_root_id, last_version, aggregate_root_payload, owned_by, belongs_to)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(aggregateRootSql)) {
            ps.setString(1, Todo.class.getSimpleName());
            ps.setString(2, "Alban/0");
            ps.setLong(3, 1);
            ps.setBytes(4, encryptedAggregatePayload);
            ps.setString(5, "Alban");
            ps.setString(6, "Alban/0");
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
            ps.setString(3, Todo.class.getSimpleName());
            ps.setString(4, "Alban/0");
            ps.setLong(5, 0);
            ps.executeUpdate();
        }

        // language=json
        final String todoMarkedAsDonePayload = """
                {}
                """;
        final byte[] encryptedTodoMarkedAsDonePayload = openPGPEncryptionService.encrypt(todoMarkedAsDonePayload.getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload();

        // When
        new ProducerBuilder<>(
                Map.of(
                        BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                        AUTO_OFFSET_RESET_CONFIG, "latest",
                        ProducerConfig.CLIENT_ID_CONFIG, "companion-" + UUID.randomUUID()),
                Duration.ofSeconds(10), new JsonNodeEventKeyObjectMapperSerializer(), new JsonNodeEventRecordObjectMapperSerializer())
                .usingGenerator(
                        integer -> new ProducerRecord<>(TOPIC,
                                new JsonNodeEventKey(Todo.class.getSimpleName(), "Alban/0", 1),
                                new JsonNodeEventValue(1_761_335_312_527L,
                                        TodoMarkedAsDone.class.getSimpleName(),
                                        encryptedTodoMarkedAsDonePayload,
                                        "Alban", "Alban/0")), 1L);

        // Then
        await().atMost(10, TimeUnit.SECONDS).until(() -> statisticsEventHandler.getCall() != null);
        assertThat(statisticsEventHandler.getCall()).isEqualTo(
                new Call(
                        new Target("statistics"),
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Alban/0"),
                        new CurrentVersionInConsumption(1),
                        Instant.ofEpochMilli(1_761_335_312_527L),
                        EventType.from(TodoMarkedAsDone.class),
                        new EncryptedPayload(encryptedTodoMarkedAsDonePayload),
                        new OwnedBy("Alban"),
                        DecryptablePayload.ofUndecryptable(),
                        new AggregateRootLoaded<>(
                                AggregateRootType.from(Todo.class),
                                new AnyAggregateId("Alban/0"),
                                new LastAggregateVersion(1),
                                new EncryptedPayload(encryptedAggregatePayload),
                                DecryptablePayload.ofUndecryptable(),
                                new OwnedBy("Alban"),
                                new BelongsTo(new AnyAggregateId("Alban/0")))));
    }
}
