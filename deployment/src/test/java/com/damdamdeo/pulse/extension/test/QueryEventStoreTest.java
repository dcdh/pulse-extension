package com.damdamdeo.pulse.extension.test;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.QueryEventStore;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
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
    void shouldReturnAggregateWhenExists() {
        // Given
        final TodoId givenTodoId = TodoId.from(new UUID(0, 0));
        final List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new NewTodoCreated(givenTodoId, "lorem ipsum")));
        todoEventRepository.save(givenTodoEvents);

        // When
        final Optional<Todo> byId = queryEventStore.findById(givenTodoId);

        // Then
        assertThat(byId).isEqualTo(Optional.of(new Todo(
                TodoId.from(new UUID(0, 0)),
                "lorem ipsum",
                Status.IN_PROGRESS,
                false
        )));
    }

    @Test
    void shouldReturnEmptyWhenAggregateDoesNotExist() {
        // Given
        final TodoId givenTodoId = TodoId.from(new UUID(0, 1));

        // When
        final Optional<Todo> byId = queryEventStore.findById(givenTodoId);

        // Then
        assertThat(byId).isEqualTo(Optional.empty());
    }
}
