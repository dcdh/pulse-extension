package com.damdamdeo.pulse.extension.consumer.deployment.aggregateroot;

import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.consumer.Producer;
import com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot.AsyncAggregateRootConsumerChannel;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

class AsyncAggregateRootConsumerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StatisticsAggregateRootHandler.class, Call.class))
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

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    OpenPGPEncryptionService openPGPEncryptionService;

    @Inject
    @Any
    Instance<StatisticsAggregateRootHandler> statisticsAggregateRootHandlerInstance;

    @Inject
    Producer producer;

    @BeforeEach
    @AfterEach
    void tearDown() {
        statisticsAggregateRootHandlerInstance.select(AsyncAggregateRootConsumerChannel.Literal.of("statistics")).get().reset();
    }

    @Test
    void shouldGenerateMessagingConfiguration() {
        assertAll(
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-aggregate-root-in.group.id", String.class))
                        .isEqualTo("TodoTaking_Todo"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-aggregate-root-in.enable.auto.commit", String.class))
                        .isEqualTo("true"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-aggregate-root-in.auto.offset.reset", String.class))
                        .isEqualTo("earliest"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-aggregate-root-in.connector", String.class))
                        .isEqualTo("smallrye-kafka"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-aggregate-root-in.topic", String.class))
                        .isEqualTo("pulse.todotaking_todo.aggregate_root"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-aggregate-root-in.key.deserializer", String.class))
                        .isEqualTo("com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot.JsonNodeAggregateRootKeyDeserializer"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-aggregate-root-in.value.deserializer", String.class))
                        .isEqualTo("com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot.JsonNodeAggregateRootValueDeserializer"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-aggregate-root-in.value.deserializer.key-type", String.class))
                        .isEqualTo("com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot.JsonNodeAggregateRootKey"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-aggregate-root-in.value.deserializer.value-type", String.class))
                        .isEqualTo("com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot.JsonNodeAggregateRootValue")
        );
    }

    @Test
    void shouldConsumeAggregateRoot() {
        // from PostgresAggregateRootLoaderTest#shouldReturnAggregate
        // Given
        final StatisticsAggregateRootHandler statisticsAggregateRootHandler = statisticsAggregateRootHandlerInstance.select(
                AsyncAggregateRootConsumerChannel.Literal.of("statistics")).get();

        // When
        final EncryptedPayload payload = producer.produceAggregateRoot(
                "statistics",
                new FromApplication("TodoTaking", "Todo"),
                // language=json
                """
                        {
                          "id": "Damien/0",
                          "description": "lorem ipsum",
                          "status": "DONE",
                          "important": false
                        }
                        """,
                new AnyAggregateId("Damien/0"),
                new OwnedBy("Damien"),
                new BelongsTo(new AnyAggregateId("Damien/0")),
                Todo.class);

        // Then
        await().atMost(10, TimeUnit.SECONDS).until(() -> statisticsAggregateRootHandler.getCall() != null);
        final ObjectNode expectedAggregateRootPayload = objectMapper.createObjectNode();
        expectedAggregateRootPayload.put("id", "Damien/0");
        expectedAggregateRootPayload.put("description", "lorem ipsum");
        expectedAggregateRootPayload.put("status", "DONE");
        expectedAggregateRootPayload.put("important", false);
        assertThat(statisticsAggregateRootHandler.getCall()).isEqualTo(
                new Call(
                        new FromApplication("TodoTaking", "Todo"),
                        new Purpose("statistics"),
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Damien/0"),
                        new CurrentVersionInConsumption(0),
                        payload,
                        new OwnedBy("Damien"),
                        new BelongsTo(new AnyAggregateId("Damien/0")),
                        DecryptablePayload.ofDecrypted(expectedAggregateRootPayload)));
    }

    @Test
    void shouldConsumeAggregateRootWhenPassPhraseDoesNotExistsAnymore() {
        // from PostgresAggregateRootLoaderTest#shouldReturnAggregate
        // Given
        final StatisticsAggregateRootHandler statisticsAggregateRootHandler = statisticsAggregateRootHandlerInstance.select(
                AsyncAggregateRootConsumerChannel.Literal.of("statistics")).get();

        // When
        final EncryptedPayload payload = producer.produceAggregateRoot(
                "statistics",
                new FromApplication("TodoTaking", "Todo"),
                // language=json
                """
                        {
                          "id": "Alban/0",
                          "description": "lorem ipsum",
                          "status": "DONE",
                          "important": false
                        }
                        """,
                new AnyAggregateId("Alban/0"),
                new OwnedBy("Alban"),
                new BelongsTo(new AnyAggregateId("Alban/0")),
                Todo.class);

        // Then
        await().atMost(10, TimeUnit.SECONDS).until(() -> statisticsAggregateRootHandler.getCall() != null);
        assertThat(statisticsAggregateRootHandler.getCall()).isEqualTo(
                new Call(
                        new FromApplication("TodoTaking", "Todo"),
                        new Purpose("statistics"),
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Alban/0"),
                        new CurrentVersionInConsumption(0),
                        payload,
                        new OwnedBy("Alban"),
                        new BelongsTo(new AnyAggregateId("Alban/0")),
                        DecryptablePayload.ofUndecryptable()));
    }
}
