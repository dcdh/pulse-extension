package com.damdamdeo.pulse.extension.test;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.*;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QueryEventStoreTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .withConfigurationResource("application.properties");

    @Inject
    EventRepository<Todo, TodoId> todoEventRepository;

    @Inject
    QueryEventStore<Todo, TodoId> queryEventStore;

    @Test
    void shouldFindByIdReturnAggregateWhenExists() {
        // Given
        final TodoId givenTodoId = TodoId.from(new UUID(1, 0));
        final List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new NewTodoCreated(givenTodoId, "lorem ipsum")));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        TodoId.from(new UUID(1, 0)),
                        "lorem ipsum",
                        Status.IN_PROGRESS,
                        false
                ));

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
        final TodoId givenTodoId = TodoId.from(new UUID(1, 1));

        // When
        final Optional<Todo> byId = queryEventStore.findById(givenTodoId);

        // Then
        assertThat(byId).isEqualTo(Optional.empty());
    }

    @Test
    void shouldFindByIdAndVersionReturnEmptyOnUnknownVersion() {
        // Given
        final TodoId givenTodoId = TodoId.from(new UUID(1, 10));
        final AggregateVersion aggregateVersion = new AggregateVersion(1);

        // When
        final Optional<Todo> byIdAndVersion = queryEventStore.findByIdAndVersion(givenTodoId, aggregateVersion);

        // Then
        assertThat(byIdAndVersion).isEqualTo(Optional.empty());
    }

    @Test
    void shouldFindByIdAndVersionUseAggregateRootTable() {
        // Given
        final TodoId givenTodoId = TodoId.from(new UUID(1, 2));
        final AggregateVersion aggregateVersion = new AggregateVersion(1);
        final List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new NewTodoCreated(givenTodoId, "lorem ipsum")),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new TodoMarkedAsDone(givenTodoId)));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        givenTodoId,
                        "lorem ipsum",
                        Status.DONE,
                        false
                ));

        // When
        final Optional<Todo> byIdAndVersion = queryEventStore.findByIdAndVersion(givenTodoId, aggregateVersion);

        // Then
        assertThat(byIdAndVersion).isEqualTo(Optional.of(
                new Todo(
                        TodoId.from(new UUID(1, 2)),
                        "lorem ipsum",
                        Status.DONE,
                        false
                )
        ));
    }

    @Test
    void shouldFindByIdAndVersionUseEventsTableWhenBelowLatestVersion() {
        // Given
        final TodoId givenTodoId = TodoId.from(new UUID(1, 3));
        final AggregateVersion aggregateVersion = new AggregateVersion(0);
        final List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new NewTodoCreated(givenTodoId, "lorem ipsum")),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new TodoMarkedAsDone(givenTodoId)));
        todoEventRepository.save(givenTodoEvents,
                new Todo(
                        givenTodoId,
                        "lorem ipsum",
                        Status.DONE,
                        false
                ));

        // When
        final Optional<Todo> byIdAndVersion = queryEventStore.findByIdAndVersion(givenTodoId, aggregateVersion);


        // Then
        assertThat(byIdAndVersion).isEqualTo(Optional.of(
                new Todo(
                        TodoId.from(new UUID(1, 3)),
                        "lorem ipsum",
                        Status.IN_PROGRESS,
                        false
                )
        ));
    }
}
