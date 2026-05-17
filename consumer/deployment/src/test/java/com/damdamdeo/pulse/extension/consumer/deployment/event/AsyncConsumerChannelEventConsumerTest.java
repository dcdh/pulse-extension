package com.damdamdeo.pulse.extension.consumer.deployment.event;

import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.consumer.Producer;
import com.damdamdeo.pulse.extension.consumer.Response;
import com.damdamdeo.pulse.extension.consumer.runtime.event.AsyncEventConsumerChannel;
import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.event.AggregateRootLoaded;
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
import java.time.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

class AsyncConsumerChannelEventConsumerTest {

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
            if (User.OWNED_BY_USER_1.equals(ownedBy)) {
                return Optional.of(PassphraseSample.PASSPHRASE);
            } else {
                return Optional.empty();
            }
        }

        @Override
        public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
            throw new IllegalStateException("Should not be called !");
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
        statisticsEventHandlerInstance.select(AsyncEventConsumerChannel.Literal.of("statistics")).get().reset();
    }

    @Test
    void shouldGenerateMessagingConfiguration() {
        assertAll(
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-event-in.group.id", String.class))
                        .isEqualTo("TodoTaking_Todo"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-event-in.enable.auto.commit", String.class))
                        .isEqualTo("true"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-event-in.auto.offset.reset", String.class))
                        .isEqualTo("earliest"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-event-in.connector", String.class))
                        .isEqualTo("smallrye-kafka"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-event-in.topic", String.class))
                        .isEqualTo("pulse.todotaking_todo.event"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-event-in.key.deserializer", String.class))
                        .isEqualTo("com.damdamdeo.pulse.extension.consumer.runtime.event.JsonNodeEventKeyDeserializer"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-event-in.value.deserializer", String.class))
                        .isEqualTo("com.damdamdeo.pulse.extension.consumer.runtime.event.JsonNodeEventValueDeserializer"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-event-in.value.deserializer.key-type", String.class))
                        .isEqualTo("com.damdamdeo.pulse.extension.consumer.runtime.event.JsonNodeEventKey"),
                () -> assertThat(ConfigProvider.getConfig().getValue("mp.messaging.incoming.statistics-todotaking-todo-event-in.value.deserializer.value-type", String.class))
                        .isEqualTo("com.damdamdeo.pulse.extension.consumer.runtime.event.JsonNodeEventValue")
        );
    }

    @Test
    void shouldConsumeEvent() {
        // from PostgresAggregateRootLoaderTest#shouldReturnAggregate
        // Given
        final StatisticsEventHandler statisticsEventHandler = statisticsEventHandlerInstance.select(
                AsyncEventConsumerChannel.Literal.of("statistics")).get();

        // When
        final Response response = producer.produceEvent(
                "statistics",
                new FromApplication("TodoTaking", "Todo"),
                // language=json
                """
                        {
                          "id": "U000001-T000001",
                          "description": "lorem ipsum",
                          "status": "DONE",
                          "important": false
                        }
                        """,
                // language=json
                """
                        {}
                        """,
                new AnyAggregateId(TodoId.USER_1_TODO_1.id()),
                User.OWNED_BY_USER_1,
                ExecutedBy.NotAvailable.INSTANCE,
                BelongsTo.from(TodoId.USER_1_TODO_1),
                Todo.class,
                TodoMarkedAsDone.class);

        // Then
        await().atMost(10, TimeUnit.SECONDS).until(() -> statisticsEventHandler.getCall() != null);
        final ObjectNode expectedTodoMarkedAsDonePayload = objectMapper.createObjectNode();
        final ObjectNode expectedAggregateRootPayload = objectMapper.createObjectNode();
        expectedAggregateRootPayload.put("id", TodoId.USER_1_TODO_1.id());
        expectedAggregateRootPayload.put("description", "lorem ipsum");
        expectedAggregateRootPayload.put("status", "DONE");
        expectedAggregateRootPayload.put("important", false);
        assertThat(statisticsEventHandler.getCall()).isEqualTo(
                new Call(
                        new FromApplication("TodoTaking", "Todo"),
                        new Purpose("statistics"),
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId(TodoId.USER_1_TODO_1.id()),
                        new CurrentVersionInConsumption(0),
                        ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12), LocalTime.of(13, 46, 40), ZoneOffset.UTC),
                        EventType.from(TodoMarkedAsDone.class),
                        response.encryptedEvent(),
                        User.OWNED_BY_USER_1,
                        BelongsTo.from(TodoId.USER_1_TODO_1),
                        ExecutedBy.NotAvailable.INSTANCE,
                        DecryptablePayload.ofDecrypted(expectedTodoMarkedAsDonePayload),
                        new AggregateRootLoaded<>(
                                AggregateRootType.from(Todo.class),
                                new AnyAggregateId(TodoId.USER_1_TODO_1.id()),
                                new LastAggregateVersion(1),
                                response.encryptedAggregateRoot(),
                                DecryptablePayload.ofDecrypted(expectedAggregateRootPayload),
                                User.OWNED_BY_USER_1,
                                BelongsTo.from(TodoId.USER_1_TODO_1))));
    }

    @Test
    void shouldConsumeEventWhenPassPhraseDoesNotExistsAnymore() {
        // from PostgresAggregateRootLoaderTest#shouldReturnAggregate
        // Given
        final StatisticsEventHandler statisticsEventHandler = statisticsEventHandlerInstance.select(
                AsyncEventConsumerChannel.Literal.of("statistics")).get();

        // When
        final Response response = producer.produceEvent(
                "statistics",
                new FromApplication("TodoTaking", "Todo"),
                // language=json
                """
                        {
                          "id": "U000002-000001",
                          "description": "lorem ipsum",
                          "status": "DONE",
                          "important": false
                        }
                        """,
                // language=json
                """
                        {}
                        """,
                new AnyAggregateId(TodoId.USER_2_TODO_1.id()),
                User.OWNED_BY_USER_2,
                ExecutedBy.NotAvailable.INSTANCE,
                BelongsTo.from(TodoId.USER_2_TODO_1),
                Todo.class,
                TodoMarkedAsDone.class);

        // Then
        await().atMost(10, TimeUnit.SECONDS).until(() -> statisticsEventHandler.getCall() != null);
        assertThat(statisticsEventHandler.getCall()).isEqualTo(
                new Call(
                        new FromApplication("TodoTaking", "Todo"),
                        new Purpose("statistics"),
                        AggregateRootType.from(Todo.class),
                        new AnyAggregateId(TodoId.USER_2_TODO_1.id()),
                        new CurrentVersionInConsumption(0),
                        ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12), LocalTime.of(13, 46, 40), ZoneOffset.UTC),
                        EventType.from(TodoMarkedAsDone.class),
                        response.encryptedEvent(),
                        User.OWNED_BY_USER_2,
                        BelongsTo.from(TodoId.USER_2_TODO_1),
                        ExecutedBy.NotAvailable.INSTANCE,
                        DecryptablePayload.ofUndecryptable(),
                        new AggregateRootLoaded<>(
                                AggregateRootType.from(Todo.class),
                                new AnyAggregateId(TodoId.USER_2_TODO_1.id()),
                                new LastAggregateVersion(1),
                                response.encryptedAggregateRoot(),
                                DecryptablePayload.ofUndecryptable(),
                                User.OWNED_BY_USER_2,
                                BelongsTo.from(TodoId.USER_2_TODO_1))));
    }
}
