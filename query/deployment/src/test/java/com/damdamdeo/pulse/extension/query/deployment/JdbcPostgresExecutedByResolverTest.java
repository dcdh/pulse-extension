package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.TodoItemAdded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.UnableToResolveException;
import com.damdamdeo.pulse.extension.query.runtime.JdbcPostgresExecutedByResolver;
import com.damdamdeo.pulse.extension.writer.runtime.serializer.EventTestRepository;
import io.quarkus.test.QuarkusUnitTest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JdbcPostgresExecutedByResolverTest {

    private static ExecutedBy BOB = new ExecutedBy.ServiceAccount("bob");
    private static ExecutedBy ALICE = new ExecutedBy.ServiceAccount("alice");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubPassphraseProvider.class,
                    TodoProjection.class, TodoChecklistProjection.class))
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Inject
    JdbcPostgresExecutedByResolver jdbcPostgresExecutedByResolver;

    @Inject
    EventTestRepository eventTestRepository;

    @Test
    @Order(1)
    void shouldStoreInDatabase() {
        // Given
        {
            final Todo todo = new Todo(
                    new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1),
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
                    new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1),
                    "Implement Projection feature");
            eventTestRepository.insert(
                    new TodoItemAdded("Implement Projection feature"),
                    todoChecklist,
                    todoChecklist.ownedBy(),
                    ALICE);
        }
        {
            final Todo todo = new Todo(
                    new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_2),
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
                    new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_2), TodoChecklistId.SEQUENCE_NUMBER_1),
                    "Go see family");
            eventTestRepository.insert(
                    new TodoItemAdded("Go see family"),
                    todoChecklist,
                    todoChecklist.ownedBy(),
                    BOB);
        }
        {
            final Todo todo = new Todo(
                    new TodoId(UserId.USER_2, TodoId.SEQUENCE_NUMBER_1),
                    "Bob vacancies",
                    Status.IN_PROGRESS,
                    false);
            eventTestRepository.insert(
                    new NewTodoCreated("Bob vacancies"),
                    todo,
                    todo.ownedBy(),
                    BOB);
        }

        // When
        final List<String> stored = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement("SELECT aggregate_root_id, executed_by, owned_by FROM aggregate_executed_by");
             final ResultSet resultSet = ps.executeQuery()) {
            while (resultSet.next()) {
                stored.add(resultSet.getString("aggregate_root_id") + "|" + resultSet.getString("executed_by") + "|" + resultSet.getString("owned_by"));
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        // Then
        assertThat(stored).containsExactly("U000001-T000001|SA:bob|U000001",
                "U000001-T000001-CL000001|SA:alice|U000001",
                "U000001-T000002|SA:bob|U000001",
                "U000001-T000002-CL000001|SA:bob|U000001",
                "U000002-T000001|SA:bob|U000002");
    }

    @Test
    @Order(2)
    void shouldResolve() throws UnableToResolveException {
        // Given

        // When
        final Set<ExecutedBy> resolved = jdbcPostgresExecutedByResolver.resolve(Set.of(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1),
                new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1)));

        // Then
        assertThat(resolved).containsExactly(BOB, ALICE);
    }
}
