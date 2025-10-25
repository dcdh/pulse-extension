package com.damdamdeo.pulse.extension.test.consumer;

import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.LastAggregateVersion;
import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.consumer.AggregateRootLoaded;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.consumer.InRelationWith;
import com.damdamdeo.pulse.extension.core.consumer.UnknownAggregateRootException;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.runtime.consumer.*;
import com.damdamdeo.pulse.extension.runtime.encryption.*;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresAggregateRootLoaderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-messaging-kafka", Version.getVersion())))
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
    DataSource dataSource;

    @Test
    void shouldThrowUnknownAggregateRootExceptionWhenAggregateDoesNotExists() {
        // Given

        // When
        assertThatThrownBy(() ->
                postgresAggregateRootLoader.getByAggregateRootTypeAndAggregateId(
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Damien/Unknown")))
                .isExactlyInstanceOf(UnknownAggregateRootException.class)
                .hasFieldOrPropertyWithValue("aggregateRootType", AggregateRootType.from(Todo.class))
                .hasFieldOrPropertyWithValue("aggregateId", new AnyAggregateId("Damien/Unknown"));
    }

    @Test
    void shouldReturnAggregate() {
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
        final AggregateRootLoaded byAggregateRootTypeAndAggregateId = postgresAggregateRootLoader.getByAggregateRootTypeAndAggregateId(AggregateRootType.from(Todo.class),
                new AnyAggregateId("Damien/0"));

        // Then
        assertThat(byAggregateRootTypeAndAggregateId).isEqualTo(
                new AggregateRootLoaded(
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Damien/0"),
                        new LastAggregateVersion(0),
                        new EncryptedPayload(payload),
                        new DecryptedPayload("Hello world!".getBytes(StandardCharsets.UTF_8)),
                        new OwnedBy("Damien"),
                        new InRelationWith("Damien/0")));
    }
}
