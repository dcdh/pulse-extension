package com.damdamdeo.pulse.extension.consumer;

import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoder;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.smallrye.reactive.messaging.kafka.companion.ProducerBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;

@ApplicationScoped
public class Producer {

    public static final Passphrase PASSPHRASE = new Passphrase("7-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&".toCharArray());

    @Inject
    DataSource dataSource;

    @Inject
    OpenPGPEncryptionService openPGPEncryptionService;

    // TODO support chaining events
    public <A extends AggregateRoot<?>, B extends Event> Response produceEvent(final String target,
                                                                               final FromApplication fromApplication,
                                                                               final String aggregateRootPayload,
                                                                               final String eventPayload,
                                                                               final AggregateId aggregateId,
                                                                               final OwnedBy ownedBy,
                                                                               final ExecutedBy executedBy,
                                                                               final BelongsTo belongsTo,
                                                                               final Class<A> aggregateRootClass,
                                                                               final Class<B> eventClass) {
        // from PostgresAggregateRootLoaderTest#shouldReturnAggregate
        // Given
        final byte[] encryptedAggregatePayload = openPGPEncryptionService.encrypt(aggregateRootPayload.getBytes(StandardCharsets.UTF_8), PASSPHRASE).payload();
        // language=sql
        final String aggregateRootSql = """
                    INSERT INTO todotaking_todo.aggregate_root (aggregate_root_type, aggregate_root_id, last_version, aggregate_root_payload, owned_by, belongs_to)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(aggregateRootSql)) {
            ps.setString(1, aggregateRootClass.getSimpleName());
            ps.setString(2, aggregateId.id());
            ps.setLong(3, 1);
            ps.setBytes(4, encryptedAggregatePayload);
            ps.setString(5, ownedBy.id());
            ps.setString(6, belongsTo.aggregateId().id());
            ps.executeUpdate();
        } catch (final SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }

        final byte[] encryptedPayload = openPGPEncryptionService.encrypt(eventPayload.getBytes(StandardCharsets.UTF_8), PASSPHRASE).payload();

        // When
        new ProducerBuilder<>(
                Map.of(
                        BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                        AUTO_OFFSET_RESET_CONFIG, "latest",
                        ProducerConfig.CLIENT_ID_CONFIG, "companion-" + UUID.randomUUID()),
                Duration.ofSeconds(10), new ObjectMapperSerializer<JsonNodeEventKey>(), new ObjectMapperSerializer<JsonNodeEventValue>())
                .usingGenerator(
                        integer -> new ProducerRecord<>("pulse.%s_%s.event".formatted(fromApplication.functionalDomain().toLowerCase(), fromApplication.componentName().toLowerCase()),
                                new JsonNodeEventKey(aggregateRootClass.getSimpleName(), aggregateId.id(), 0),
                                new JsonNodeEventValue(1_761_335_312_527L * 1000,
                                        eventClass.getSimpleName(),
                                        encryptedPayload,
                                        ownedBy.id(),
                                        belongsTo.aggregateId().id(),
                                        executedBy.encode(new ExecutedByEncoder() {
                                            @Override
                                            public byte[] encode(String value) {
                                                return ("encoded" + value).getBytes(StandardCharsets.UTF_8);
                                            }
                                        }))), 1L);
        return new Response(
                new EncryptedPayload(encryptedAggregatePayload),
                new EncryptedPayload(encryptedPayload));
    }

    // TODO support chaining events
    public <A extends AggregateRoot<?>> EncryptedPayload produceAggregateRoot(final String target,
                                                                              final FromApplication fromApplication,
                                                                              final String aggregateRootPayload,
                                                                              final AggregateId aggregateId,
                                                                              final OwnedBy ownedBy,
                                                                              final BelongsTo belongsTo,
                                                                              final Class<A> aggregateRootClass) {
        // Given
        final byte[] encryptedAggregatePayload = openPGPEncryptionService.encrypt(aggregateRootPayload.getBytes(StandardCharsets.UTF_8), PASSPHRASE).payload();
        // language=sql
        final String aggregateRootSql = """
                    INSERT INTO %s.aggregate_root (aggregate_root_type, aggregate_root_id, last_version, aggregate_root_payload, owned_by, belongs_to)
                    VALUES (?, ?, ?, ?, ?, ?)
                """.formatted(fromApplication.value());
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(aggregateRootSql)) {
            ps.setString(1, aggregateRootClass.getSimpleName());
            ps.setString(2, aggregateId.id());
            ps.setLong(3, 1);
            ps.setBytes(4, encryptedAggregatePayload);
            ps.setString(5, ownedBy.id());
            ps.setString(6, belongsTo.aggregateId().id());
            ps.executeUpdate();
        } catch (final SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }

        // When
        final byte[] encryptedPayload = openPGPEncryptionService.encrypt(aggregateRootPayload.getBytes(StandardCharsets.UTF_8), PASSPHRASE).payload();

        new ProducerBuilder<>(
                Map.of(
                        BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                        AUTO_OFFSET_RESET_CONFIG, "latest",
                        ProducerConfig.CLIENT_ID_CONFIG, "companion-" + UUID.randomUUID()),
                Duration.ofSeconds(10), new ObjectMapperSerializer<JsonNodeAggregateRootKey>(), new ObjectMapperSerializer<JsonNodeAggregateRootValue>())
                .usingGenerator(
                        integer -> new ProducerRecord<>("pulse.%s_%s.aggregate_root".formatted(fromApplication.functionalDomain().toLowerCase(), fromApplication.componentName().toLowerCase()),
                                new JsonNodeAggregateRootKey(aggregateRootClass.getSimpleName(), aggregateId.id(), 0),
                                new JsonNodeAggregateRootValue(1L,
                                        encryptedPayload,
                                        ownedBy.id(),
                                        belongsTo.aggregateId().id())));
        return new EncryptedPayload(encryptedPayload);
    }
}
