package com.damdamdeo.pulse.extension.test;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.runtime.InstantProvider;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcEventRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .withConfigurationResource("application.properties");

    @Inject
    EventRepository<Todo, TodoId> todoEventRepository;

    @Inject
    DataSource dataSource;

    @ApplicationScoped
    static class StubInstantProvider implements InstantProvider {

        @Override
        public Instant now() {
            return Instant.parse("2025-10-13T18:00:00Z");
        }
    }

    @Test
    @Order(1)
    void shouldSave() {
        // Given
        List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new NewTodoCreated(TodoId.from(new UUID(0, 0)), "lorem ipsum")));

        // When
        todoEventRepository.save(givenTodoEvents);

        // Then
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT aggregaterootid, aggregateroottype, version, creationdate, eventtype, eventpayload
                                 FROM public.t_event
                             """);
             final ResultSet rs = ps.executeQuery()) {
            rs.next();
            assertAll(
                    () -> assertThat(rs.getString("aggregaterootid")).isEqualTo("00000000-0000-0000-0000-000000000000"),
                    () -> assertThat(rs.getString("aggregateroottype")).isEqualTo("com.damdamdeo.pulse.extension.core.Todo"),
                    () -> assertThat(rs.getLong("version")).isEqualTo(0),
                    () -> assertThat(rs.getString("creationdate")).isEqualTo("2025-10-13 20:00:00"),
                    () -> assertThat(rs.getString("eventtype")).isEqualTo("com.damdamdeo.pulse.extension.core.NewTodoCreated"),
                    () -> assertThat(rs.getString("eventpayload")).isEqualTo("{\"id\": {\"id\": \"00000000-0000-0000-0000-000000000000\"}, \"description\": \"lorem ipsum\"}"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Must have the property *quarkus.class-loading.parent-first-artifacts* defined to *com.damdamdeo:pulse-core:tests*.
     * Events created from store will not belong to the same classloader as events created in this test.
     */
    @Test
    @Order(2)
    void shouldLoadOrderByVersionASC() {
        // Given
        List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                new VersionizedEvent<>(new AggregateVersion(0),
                        new NewTodoCreated(TodoId.from(new UUID(0, 1)), "lorem ipsum 2")),
                new VersionizedEvent<>(new AggregateVersion(1),
                        new TodoMarkedAsDone(TodoId.from(new UUID(0, 1)))));
        todoEventRepository.save(givenTodoEvents);

        // When
        final List<Event<TodoId>> events = todoEventRepository.loadOrderByVersionASC(TodoId.from(new UUID(0, 1)));

        // Then
        assertThat(events).containsExactly(
                new NewTodoCreated(TodoId.from(new UUID(0, 1)), "lorem ipsum 2"),
                new TodoMarkedAsDone(TodoId.from(new UUID(0, 1))));
    }
}
