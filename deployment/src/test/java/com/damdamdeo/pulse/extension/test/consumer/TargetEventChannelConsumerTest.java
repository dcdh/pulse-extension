package com.damdamdeo.pulse.extension.test.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.consumer.*;
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
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.awaitility.Awaitility.await;

@QuarkusTestResource(KafkaCompanionResource.class)
class TargetEventChannelConsumerTest {
    // FCK PRIO 1
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .overrideConfigKey("pulse.target-topic-binding.statistics", "statistics")// FCK use a proper name
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

    // FCK PRIO 1 null: analyser comment c'est fait et fournir le tiens ... passer par kafka-native
    @InjectKafkaCompanion
    KafkaCompanion kafkaCompanion;

    @ConfigProperty(name = "quarkus.application.name")
    String name;

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
        kafkaCompanion.produce(EventKey.class, EventRecord.class)
                .usingGenerator(integer -> new ProducerRecord<>("%s_t_event".formatted(name),
                        new JsonNodeEventKey("Damien/0", Todo.class.getName(), 0),
                        new JsonNodeEventRecord("Damien/0", Todo.class.getName(), 0, 1_761_335_312_527L,
                                NewTodoCreated.class.getName(),
                                OpenPGPEncryptionService.encrypt("Hello world!".getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload(),
                                "Damien")));

        // Then
        await().atMost(10, TimeUnit.SECONDS).until(() -> statisticsEventHandler.getCall() != null);
//        assertThat()
// FCK asserter le contenu recu ...
// FCK        je suis censé le consommer ... passer par awaitility ... boucler sur statisticsEventHandler ...
//
//  FCK      la je dois avoir un event handler
//                FCK je dois feed la table t_aggregate cf précédent test pour tseter le Supplier !
    }
}
