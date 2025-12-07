package com.damdamdeo.pulse.extension.consumer;

import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    OpenPGPEncryptionService openPGPEncryptionService;

    public <A extends AggregateRoot<?>, B extends Event> Response produce(final String target,
                                                                          final FromApplication fromApplication,
                                                                          final String aggregateRootPayload,
                                                                          final String eventPayload,
                                                                          final AggregateId aggregateId,
                                                                          final OwnedBy ownedBy,
                                                                          final BelongsTo belongsTo,
                                                                          final Class<A> aggregateRootClass,
                                                                          final Class<B> eventClass) {
        // from PostgresAggregateRootLoaderTest#shouldReturnAggregate
        // Given
        final byte[] encryptedAggregatePayload = openPGPEncryptionService.encrypt(aggregateRootPayload.getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload();
        // language=sql
        final String aggregateRootSql = """
                    INSERT INTO todotaking_todo.t_aggregate_root (aggregate_root_type, aggregate_root_id, last_version, aggregate_root_payload, owned_by, belongs_to)
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

        // language=sql
        final String idempotencySql = """
                INSERT INTO t_idempotency (target, from_application, aggregate_root_type, aggregate_root_id, last_consumed_version)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(idempotencySql)) {
            ps.setString(1, target);
            ps.setString(2, new FromApplication("TodoTaking", "Todo").value());
            ps.setString(3, aggregateRootClass.getSimpleName());
            ps.setString(4, aggregateId.id());
            ps.setLong(5, 0);
            ps.executeUpdate();
        } catch (final SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }

        final byte[] encryptedEvent = openPGPEncryptionService.encrypt(eventPayload.getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload();

        // When
        new ProducerBuilder<>(
                Map.of(
                        BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class),
                        AUTO_OFFSET_RESET_CONFIG, "latest",
                        ProducerConfig.CLIENT_ID_CONFIG, "companion-" + UUID.randomUUID()),
                Duration.ofSeconds(10), new JsonNodeEventKeyObjectMapperSerializer(), new JsonNodeEventRecordObjectMapperSerializer())
                .usingGenerator(
                        integer -> new ProducerRecord<>("pulse.%s_%s.t_event".formatted(fromApplication.functionalDomain().toLowerCase(), fromApplication.componentName().toLowerCase()),
                                new JsonNodeEventKey(aggregateRootClass.getSimpleName(), aggregateId.id(), 1),
                                new JsonNodeEventValue(1_761_335_312_527L,
                                        eventClass.getSimpleName(),
                                        encryptedEvent,
                                        ownedBy.id(), belongsTo.aggregateId().id())), 1L);
        return new Response(
                new EncryptedPayload(encryptedAggregatePayload),
                new EncryptedPayload(encryptedEvent));
    }
}
