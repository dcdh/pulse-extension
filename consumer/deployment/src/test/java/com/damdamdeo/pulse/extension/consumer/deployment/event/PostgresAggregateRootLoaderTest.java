package com.damdamdeo.pulse.extension.consumer.deployment.event;

import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.consumer.runtime.event.PostgresAggregateRootLoader;
import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.event.AggregateRootLoaded;
import com.damdamdeo.pulse.extension.core.consumer.event.UnknownAggregateRootException;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresAggregateRootLoaderTest {

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
            return Optional.of(PassphraseSample.PASSPHRASE);
        }

        @Override
        public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
            throw new IllegalStateException("Should not be called");
        }
    }

    @Inject
    PostgresAggregateRootLoader postgresAggregateRootLoader;

    @Inject
    OpenPGPEncryptionService openPGPEncryptionService;

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void shouldThrowUnknownAggregateRootExceptionWhenAggregateDoesNotExists() {
        // Given
        final FromApplication givenFromApplication = new FromApplication("TodoTaking", "Todo");
        final AggregateRootType givenAggregateRootType = AggregateRootType.from(Todo.class);
        final AggregateId givenAggregateId = new AnyAggregateId("Damien/Unknown");

        // When
        assertThatThrownBy(() ->
                postgresAggregateRootLoader.getByApplicationNamingAndAggregateRootTypeAndAggregateId(
                        givenFromApplication, givenAggregateRootType, givenAggregateId))
                .isExactlyInstanceOf(UnknownAggregateRootException.class)
                .hasFieldOrPropertyWithValue("aggregateRootType", AggregateRootType.from(Todo.class))
                .hasFieldOrPropertyWithValue("aggregateId", new AnyAggregateId("Damien/Unknown"));
    }

    @Test
    void shouldReturnAggregate() {
        // Given
        // language=json
        final String payload = """
                {
                  "id": "Damien/0",
                  "description": "lorem ipsum",
                  "status": "DONE",
                  "important": false
                }
                """;
        final byte[] encryptedPayload = openPGPEncryptionService.encrypt(payload.getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload();
        // language=sql
        final String sql = """
                    INSERT INTO todotaking_todo.t_aggregate_root (aggregate_root_id, aggregate_root_type, last_version, aggregate_root_payload, owned_by, belongs_to)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "Damien/0");
            ps.setString(2, Todo.class.getSimpleName());
            ps.setLong(3, 1);
            ps.setBytes(4, encryptedPayload);
            ps.setString(5, "Damien");
            ps.setString(6, "Damien/0");
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        final FromApplication givenFromApplication = new FromApplication("TodoTaking", "Todo");
        final AggregateRootType givenAggregateRootType = AggregateRootType.from(Todo.class);
        final AggregateId givenAggregateId = new AnyAggregateId("Damien/0");

        // When
        final AggregateRootLoaded<JsonNode> byAggregateRootTypeAndAggregateId = postgresAggregateRootLoader.getByApplicationNamingAndAggregateRootTypeAndAggregateId(
                givenFromApplication, givenAggregateRootType, givenAggregateId);

        // Then
        final ObjectNode expectedAggregateRootPayload = objectMapper.createObjectNode();
        expectedAggregateRootPayload.put("id", "Damien/0");
        expectedAggregateRootPayload.put("description", "lorem ipsum");
        expectedAggregateRootPayload.put("status", "DONE");
        expectedAggregateRootPayload.put("important", false);
        assertThat(byAggregateRootTypeAndAggregateId).isEqualTo(
                new AggregateRootLoaded<>(
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Damien/0"),
                        new LastAggregateVersion(1),
                        new EncryptedPayload(encryptedPayload),
                        DecryptablePayload.ofDecrypted(expectedAggregateRootPayload),
                        new OwnedBy("Damien"),
                        new BelongsTo(new AnyAggregateId("Damien/0"))));
    }
}
