package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.encryption.*;
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
            .withConfigurationResource("application.properties");

    @Inject
    EventRepository<Todo, TodoId> todoEventRepository;

    @Inject
    QueryEventStore<Todo, TodoId> queryEventStore;

    @ApplicationScoped
    static class StubPassphraseProvider implements PassphraseProvider {

        @Override
        public Passphrase provide(final OwnedBy ownedBy) {
            return PassphraseSample.PASSPHRASE_1;
        }

        @Override
        public Passphrase ban(final OwnedBy ownedBy) throws UnableToBanPassphraseException {
            throw new IllegalStateException("Should not be called");
        }
    }

    @ApplicationScoped
    static class StubPassphraseRepository implements PassphraseRepository {

        @Override
        public Optional<Passphrase> findBy(OwnedBy ownedBy) {
            return Optional.of(PassphraseSample.PASSPHRASE_1);
        }

        @Override
        public Passphrase get(final OwnedBy ownedBy) throws UnableToRetrievePassphraseException, UnknownPassphraseException {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public List<RetrievedPassphrase> list(List<OwnedBy> multiples) throws UnableToRetrievePassphraseException {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
            throw new IllegalStateException("Should not be calld !");
        }

        @Override
        public Passphrase update(final OwnedBy ownedBy, final Passphrase passphrase) throws UnableToStorePassphraseException, UnknownPassphraseException {
            throw new IllegalStateException("Should not be called");
        }
    }

    @Test
    void shouldFindByIdReturnAggregateWhenExists() {
        // Given
        final TodoId givenTodoId = new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_10);
        final List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)));
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
        final TodoId givenTodoId = new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_11);

        // When
        final Optional<Todo> byId = queryEventStore.findById(givenTodoId);

        // Then
        assertThat(byId).isEqualTo(Optional.empty());
    }

    @Test
    void shouldFindByIdAndVersionReturnEmptyOnUnknownVersion() {
        // Given
        final TodoId givenTodoId = new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_12);
        final AggregateVersion aggregateVersion = new AggregateVersion(1);

        // When
        final Optional<Todo> byIdAndVersion = queryEventStore.findByIdAndVersion(givenTodoId, aggregateVersion);

        // Then
        assertThat(byIdAndVersion).isEqualTo(Optional.empty());
    }

    @Test
    void shouldFindByIdAndVersionUseAggregateRootTable() {
        // Given
        final TodoId givenTodoId = new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_13);
        final AggregateVersion aggregateVersion = new AggregateVersion(1);
        final List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE)));
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
                        new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_13),
                        "lorem ipsum",
                        Status.DONE,
                        false
                )
        ));
    }

    @Test
    void shouldFindByIdAndVersionUseEventsTableWhenBelowLatestVersion() {
        // Given
        final TodoId givenTodoId = new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_14);
        final AggregateVersion aggregateVersion = new AggregateVersion(0);
        final List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE)));
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
                        new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_14),
                        "lorem ipsum",
                        Status.IN_PROGRESS,
                        false
                )
        ));
    }
}
