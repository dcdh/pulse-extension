package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.consumer.Producer;
import com.damdamdeo.pulse.extension.consumer.Response;
import com.damdamdeo.pulse.extension.consumer.runtime.EventChannel;
import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.event.TodoMarkedAsDone;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
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
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

class TargetEventChannelConsumerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StatisticsEventHandler.class, Call.class))
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
    Instance<StatisticsEventHandler> statisticsEventHandlerInstance;

    @Inject
    Producer producer;

    @BeforeEach
    @AfterEach
    void tearDown() {
        statisticsEventHandlerInstance.select(EventChannel.Literal.of("statistics")).get().reset();
    }

    @Test
    void shouldGenerateMessagingConfiguration() {
        assertAll(
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-in.group.id", String.class))
                        .isEqualTo("TodoTaking_Todo"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-in.enable.auto.commit", String.class))
                        .isEqualTo("true"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-in.auto.offset.reset", String.class))
                        .isEqualTo("earliest"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-in.connector", String.class))
                        .isEqualTo("smallrye-kafka"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-in.topic", String.class))
                        .isEqualTo("pulse.todotaking_todo.t_event"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-in.key.deserializer", String.class))
                        .isEqualTo("com.damdamdeo.pulse.extension.consumer.runtime.JsonNodeEventKeyObjectMapperDeserializer"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-in.value.deserializer", String.class))
                        .isEqualTo("com.damdamdeo.pulse.extension.consumer.runtime.JsonNodeEventRecordObjectMapperDeserializer"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-in.value.deserializer.key-type", String.class))
                        .isEqualTo("com.damdamdeo.pulse.extension.core.consumer.EventKey"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-in.value.deserializer.value-type", String.class))
                        .isEqualTo("com.damdamdeo.pulse.extension.core.consumer.EventValue")
        );
    }

    @Test
    void shouldConsumeEvent() {
        // from PostgresAggregateRootLoaderTest#shouldReturnAggregate
        // Given
        final StatisticsEventHandler statisticsEventHandler = statisticsEventHandlerInstance.select(
                EventChannel.Literal.of("statistics")).get();

        // When
        final Response response = producer.produce(
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
                // language=json
                """
                        {}
                        """,
                new AnyAggregateId("Damien/0"),
                new OwnedBy("Damien"),
                ExecutedBy.NotAvailable.INSTANCE,
                new BelongsTo(new AnyAggregateId("Damien/0")),
                Todo.class,
                TodoMarkedAsDone.class);

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
                        new FromApplication("TodoTaking", "Todo"),
                        new Target("statistics"),
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Damien/0"),
                        new CurrentVersionInConsumption(1),
                        Instant.ofEpochMilli(1_761_335_312_527L),
                        EventType.from(TodoMarkedAsDone.class),
                        response.encryptedEvent(),
                        new OwnedBy("Damien"),
                        ExecutedBy.NotAvailable.INSTANCE,
                        DecryptablePayload.ofDecrypted(expectedTodoMarkedAsDonePayload),
                        new AggregateRootLoaded<>(
                                AggregateRootType.from(Todo.class),
                                new AnyAggregateId("Damien/0"),
                                new LastAggregateVersion(1),
                                response.encryptedAggregateRoot(),
                                DecryptablePayload.ofDecrypted(expectedAggregateRootPayload),
                                new OwnedBy("Damien"),
                                new BelongsTo(new AnyAggregateId("Damien/0")))));
    }

    @Test
    void shouldConsumeEventWhenPassPhraseDoesNotExistsAnymore() {
        // from PostgresAggregateRootLoaderTest#shouldReturnAggregate
        // Given
        final StatisticsEventHandler statisticsEventHandler = statisticsEventHandlerInstance.select(
                EventChannel.Literal.of("statistics")).get();

        // When
        final Response response = producer.produce(
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
                // language=json
                """
                        {}
                        """,
                new AnyAggregateId("Alban/0"),
                new OwnedBy("Alban"),
                ExecutedBy.NotAvailable.INSTANCE,
                new BelongsTo(new AnyAggregateId("Alban/0")),
                Todo.class,
                TodoMarkedAsDone.class);

        // Then
        await().atMost(10, TimeUnit.SECONDS).until(() -> statisticsEventHandler.getCall() != null);
        assertThat(statisticsEventHandler.getCall()).isEqualTo(
                new Call(
                        new FromApplication("TodoTaking", "Todo"),
                        new Target("statistics"),
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId("Alban/0"),
                        new CurrentVersionInConsumption(1),
                        Instant.ofEpochMilli(1_761_335_312_527L),
                        EventType.from(TodoMarkedAsDone.class),
                        response.encryptedEvent(),
                        new OwnedBy("Alban"),
                        ExecutedBy.NotAvailable.INSTANCE,
                        DecryptablePayload.ofUndecryptable(),
                        new AggregateRootLoaded<>(
                                AggregateRootType.from(Todo.class),
                                new AnyAggregateId("Alban/0"),
                                new LastAggregateVersion(1),
                                response.encryptedAggregateRoot(),
                                DecryptablePayload.ofUndecryptable(),
                                new OwnedBy("Alban"),
                                new BelongsTo(new AnyAggregateId("Alban/0")))));
    }
}
