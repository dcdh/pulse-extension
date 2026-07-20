package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.TodoItemAdded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.TodoChecklistProjection;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.query.runtime.EventCounterException;
import com.damdamdeo.pulse.extension.query.runtime.JdbcEventCounter;
import com.damdamdeo.pulse.extension.writer.runtime.serializer.EventTestRepository;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JdbcEventCounterTest {

    // TODO should mutualise
    private static ExecutedBy BOB = new ExecutedBy.ServiceAccount("bob");
    private static ExecutedBy ALICE = new ExecutedBy.ServiceAccount("alice");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubPassphraseProvider.class,
                    TodoProjection.class, TodoChecklistProjection.class))
            .withConfigurationResource("application.properties");

    @Inject
    EventTestRepository eventTestRepository;

    @Inject
    JdbcEventCounter jdbcEventCounter;

    @Inject
    DataSource dataSource;

    @Test
    void shouldTriggerUpdateCounters() throws EventCounterException {
        // Given
        {
            final Todo todo = new Todo(
                    TodoId.USER_1_TODO_1,
                    "IMPORTANT: pulse extension development",
                    Status.IN_PROGRESS,
                    true);
            eventTestRepository.insert(
                    new NewTodoCreated("IMPORTANT: pulse extension development"),
                    todo,
                    todo.ownedBy(),
                    BOB);
        }
        {
            final TodoChecklist todoChecklist = new TodoChecklist(
                    TodoChecklistId.USER_1_TODO_1_1,
                    "Implement Projection feature");
            eventTestRepository.insert(
                    new TodoItemAdded("Implement Projection feature"),
                    todoChecklist,
                    todoChecklist.ownedBy(),
                    ALICE);
        }
        {
            final Todo todo = new Todo(
                    TodoId.USER_1_TODO_2,
                    "Organization vacancies",
                    Status.IN_PROGRESS,
                    false);
            eventTestRepository.insert(
                    new NewTodoCreated("Organization vacancies"),
                    todo,
                    todo.ownedBy(),
                    BOB);
        }
        {
            final TodoChecklist todoChecklist = new TodoChecklist(
                    TodoChecklistId.USER_1_TODO_2_1,
                    "Go see family");
            eventTestRepository.insert(
                    new TodoItemAdded("Go see family"),
                    todoChecklist,
                    todoChecklist.ownedBy(),
                    BOB);
        }
        final Todo todo = new Todo(
                TodoId.USER_2_TODO_1,
                "Bob vacancies",
                Status.IN_PROGRESS,
                false);
        eventTestRepository.insert(
                new NewTodoCreated("Bob vacancies"),
                todo,
                todo.ownedBy(),
                BOB);

        // When
        final Integer byAggregateId = jdbcEventCounter.byAggregateId(TodoId.USER_1_TODO_2);
        final Integer byOwnedBy = jdbcEventCounter.byOwnedBy(todo.ownedBy());

        final List<String> counts = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement("SELECT counter_type, counter_id, event_count FROM event_counter");
             final ResultSet resultSet = ps.executeQuery()) {
            while (resultSet.next()) {
                counts.add(resultSet.getString("counter_type") + "|" + resultSet.getString("counter_id") + "|" + resultSet.getInt("event_count"));
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        assertAll(
                () -> assertThat(byAggregateId).isEqualTo(1),
                () -> assertThat(byOwnedBy).isEqualTo(1),
                () -> assertThat(counts).containsExactly("AGGREGATE_ID|U000001-T000001|1",
                        "AGGREGATE_ID|U000001-T000001-CL000001|1",
                        "AGGREGATE_ID|U000001-T000002|1",
                        "AGGREGATE_ID|U000001-T000002-CL000001|1",
                        "OWNED_BY|U000001|4",
                        "AGGREGATE_ID|U000002-T000001|1",
                        "OWNED_BY|U000002|1")
        );
    }
}
