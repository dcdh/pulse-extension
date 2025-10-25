package com.damdamdeo.pulse.extension.test.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.consumer.AggregateRootLoaded;
import com.damdamdeo.pulse.extension.core.consumer.AsyncEventChannelMessageHandler;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.runtime.consumer.EventChannel;
import com.damdamdeo.pulse.extension.runtime.consumer.JsonNodeEventKey;
import com.damdamdeo.pulse.extension.runtime.consumer.JsonNodeEventRecord;
import com.damdamdeo.pulse.extension.runtime.encryption.OpenPGPEncryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.builder.Version;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.reactive.messaging.kafka.companion.ProducerBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
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
import static org.apache.kafka.clients.CommonClientConfigs.METADATA_MAX_AGE_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class TargetEventChannelConsumerTest {
    // FCK PRIO 1
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .overrideConfigKey("pulse.target-topic-binding.statistics", "statistics")// FCK use a proper name
            .overrideConfigKey("debezium.enabled", "false")
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

    // FCK ICI je dois produire un message dans le kafka (sans debezium TODO ajouter une option pour le desactiver) comme cela je peux le tester
    // et tester que l'appel a bien été fait en checkant la consommation du message (via la presence d'un element dans la table idempotency)
    // et checker que l'appel a bien été effectué ... je dois tous stocker puis verifier via mon Stub !
    // objectif tester la conf générée ...

    record Call(Target target,
                AggregateId aggregateId,
                AggregateRootType aggregateRootType,
                CurrentVersionInConsumption currentVersionInConsumption,
                Instant creationDate,
                EventType eventType,
                EncryptedPayload encryptedPayload,
                OwnedBy ownedBy,
                JsonNode decryptedEventPayload,
                AggregateRootLoaded<JsonNode> aggregateRootLoaded) {
        Call {
            Objects.requireNonNull(target);
            Objects.requireNonNull(aggregateId);
            Objects.requireNonNull(aggregateRootType);
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
    @EventChannel(target = "statistics")
    static final class StatisticsEventHandler implements AsyncEventChannelMessageHandler<JsonNode> {

        private Call call = null;

        @Override
        public void handleMessage(final Target target,
                                  final AggregateId aggregateId,
                                  final AggregateRootType aggregateRootType,
                                  final CurrentVersionInConsumption currentVersionInConsumption,
                                  final Instant creationDate,
                                  final EventType eventType,
                                  final EncryptedPayload encryptedPayload,
                                  final OwnedBy ownedBy,
                                  final JsonNode decryptedEventPayload,
                                  final Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier) {
            this.call = new Call(
                    target, aggregateId, aggregateRootType, currentVersionInConsumption, creationDate, eventType,
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
    StatisticsEventHandler statisticsEventHandler;

    @ConfigProperty(name = "quarkus.application.name")
    String name;

    public static final class JsonNodeEventKeyObjectMapperSerializer extends ObjectMapperSerializer<JsonNodeEventKey> {

    }

    public static final class JsonNodeEventRecordObjectMapperSerializer extends ObjectMapperSerializer<JsonNodeEventRecord> {

    }

    @BeforeEach
    void setup() {
        final Map<String, Object> configMap = Map.of(
                BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                METADATA_MAX_AGE_CONFIG, 1000,
                CLIENT_ID_CONFIG, "companion-admin-for-" + UUID.randomUUID());
        final AdminClient adminClient = AdminClient.create(configMap);
        adminClient.createTopics(List.of(new NewTopic("%s_t_event".formatted(name), Optional.empty(), Optional.empty())));
    }
2025-10-25 18:44:15,878 WARN  [org.apa.kaf.cli.NetworkClient] (smallrye-kafka-consumer-thread-0) [Consumer clientId=kafka-consumer-statistics, groupId=pulse-extension-deployment] The metadata response from the cluster reported a recoverable issue with correlation id 2 : {statistics_t_event=UNKNOWN_TOPIC_OR_PARTITION}
2025-10-25 18:44:15,973 WARN  [org.apa.kaf.cli.NetworkClient] (kafka-producer-network-thread | companion-f8ee541e-3f37-4f30-ab76-dd813a88ff7f) [Producer clientId=companion-f8ee541e-3f37-4f30-ab76-dd813a88ff7f] The metadata response from the cluster reported a recoverable issue with correlation id 1 : {pulse-extension-deployment_t_event=UNKNOWN_TOPIC_OR_PARTITION}

    @Test
    void shouldConsumeEvent() {
        // from PostgresAggregateRootLoaderTest#shouldReturnAggregate
        // Given
        final byte[] payload = OpenPGPEncryptionService.encrypt("Hello world!".getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload();
        // language=sql
        final String sql = """
                    INSERT INTO t_aggregate_root (aggregate_root_id, aggregate_root_type, last_version, aggregate_root_payload, owned_by, in_relation_with)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "Damien/0");
            ps.setString(2, Todo.class.getName());
            ps.setLong(3, 0);
            ps.setBytes(4, payload);
            ps.setString(5, "Damien");
            ps.setString(6, "Damien/0");
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        // When
        new ProducerBuilder<>(
                Map.of(
                        BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                        ProducerConfig.CLIENT_ID_CONFIG, "companion-" + UUID.randomUUID()),
                Duration.ofSeconds(10), new JsonNodeEventKeyObjectMapperSerializer(), new JsonNodeEventRecordObjectMapperSerializer())
                .usingGenerator(
                        integer -> new ProducerRecord<>("%s_t_event".formatted(name),
                                new JsonNodeEventKey("Damien/0", Todo.class.getName(), 0),
                                new JsonNodeEventRecord("Damien/0", Todo.class.getName(), 0, 1_761_335_312_527L,
                                        NewTodoCreated.class.getName(),
                                        OpenPGPEncryptionService.encrypt("Hello world!".getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload(),
                                        "Damien")));

        // Then
        await().atMost(10, TimeUnit.SECONDS).until(() -> statisticsEventHandler.getCall() != null);
        assertThat(statisticsEventHandler.getCall()).isEqualTo("BOOM!!!");
// FCK asserter le contenu recu ...
// FCK        je suis censé le consommer ... passer par awaitility ... boucler sur statisticsEventHandler ...
//
//  FCK      la je dois avoir un event handler
//                FCK je dois feed la table t_aggregate cf précédent test pour tseter le Supplier !
    }
}
