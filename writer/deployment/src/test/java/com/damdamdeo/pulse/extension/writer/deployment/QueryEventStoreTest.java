package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class QueryEventStoreTest {

    private static ExecutedBy BOB = new ExecutedBy.EndUser("bob", true);

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .withConfigurationResource("application.properties");

    @Inject
    EventRepository<Todo, TodoId> todoEventRepository;

    @Inject
    QueryEventStore<Todo, TodoId> queryEventStore;

    @ApplicationScoped
    static class StubPassphraseProvider implements PassphraseProvider {

        @Override
        public Passphrase provide(final OwnedBy ownedBy) {
            return PassphraseSample.PASSPHRASE;
        }
    }

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

    @Test
    void shouldFindByIdReturnAggregateWhenExists() {
        // Given
        final TodoId givenTodoId = new TodoId("Damien", 10L);
        final List<VersionizedEvent> givenTodoEvents = List.of(
                new VersionizedEvent(new AggregateVersion(0),
                        new NewTodoCreated("lorem ipsum")));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        givenTodoId,
                        "lorem ipsum",
                        Status.IN_PROGRESS,
                        false
                ), BOB);

        // When
        final Optional<Todo> byId = queryEventStore.findById(givenTodoId);

        // Then
        assertThat(byId).isEqualTo(Optional.of(new Todo(
                givenTodoId,
                "lorem ipsum",
                Status.IN_PROGRESS,
                false
        )));
    }

    @Test
    void shouldFindByIdReturnEmptyWhenAggregateDoesNotExist() {
        // Given
        final TodoId givenTodoId = new TodoId("Damien", 11L);

        // When
        final Optional<Todo> byId = queryEventStore.findById(givenTodoId);

        // Then
        assertThat(byId).isEqualTo(Optional.empty());
    }

    @Test
    void shouldFindByIdAndVersionReturnEmptyOnUnknownVersion() {
        // Given
        final TodoId givenTodoId = new TodoId("Damien", 12L);
        final AggregateVersion aggregateVersion = new AggregateVersion(1);

        // When
        final Optional<Todo> byIdAndVersion = queryEventStore.findByIdAndVersion(givenTodoId, aggregateVersion);

        // Then
        assertThat(byIdAndVersion).isEqualTo(Optional.empty());
    }

    @Test
    void shouldFindByIdAndVersionUseAggregateRootTable() {
        // Given
        final TodoId givenTodoId = new TodoId("Damien", 13L);
        final AggregateVersion aggregateVersion = new AggregateVersion(1);
        final List<VersionizedEvent> givenTodoEvents = List.of(
                new VersionizedEvent(new AggregateVersion(0),
                        new NewTodoCreated("lorem ipsum")),
                new VersionizedEvent(new AggregateVersion(1),
                        new TodoMarkedAsDone()));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        givenTodoId,
                        "lorem ipsum",
                        Status.DONE,
                        false
                ), BOB);

        // When
        final Optional<Todo> byIdAndVersion = queryEventStore.findByIdAndVersion(givenTodoId, aggregateVersion);

        // Then
        assertThat(byIdAndVersion).isEqualTo(Optional.of(
                new Todo(
                        new TodoId("Damien", 13L),
                        "lorem ipsum",
                        Status.DONE,
                        false
                )
        ));
    }

    @Test
    void shouldFindByIdAndVersionUseEventsTableWhenBelowLatestVersion() {
        // Given
        final TodoId givenTodoId = new TodoId("Damien", 14L);
        final AggregateVersion aggregateVersion = new AggregateVersion(0);
        final List<VersionizedEvent> givenTodoEvents = List.of(
                new VersionizedEvent(new AggregateVersion(0),
                        new NewTodoCreated("lorem ipsum")),
                new VersionizedEvent(new AggregateVersion(1),
                        new TodoMarkedAsDone()));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        givenTodoId,
                        "lorem ipsum",
                        Status.DONE,
                        false
                ), BOB);

        // When
        final Optional<Todo> byIdAndVersion = queryEventStore.findByIdAndVersion(givenTodoId, aggregateVersion);


        // Then
        assertThat(byIdAndVersion).isEqualTo(Optional.of(
                new Todo(
                        new TodoId("Damien", 14L),
                        "lorem ipsum",
                        Status.IN_PROGRESS,
                        false
                )
        ));
    }
}
