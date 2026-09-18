package com.damdamdeo.pulse.extension.common.deployment.audience;

import com.damdamdeo.pulse.extension.common.runtime.audience.JdbcPostgresExecutedByResolver;
import com.damdamdeo.pulse.extension.common.runtime.serialization.BusinessMapper;
import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.damdamdeo.pulse.extension.core.encryption.UnableToBanPassphraseException;
import com.damdamdeo.pulse.extension.core.encryption.UnableToProvidePassphraseException;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.event.TodoItemAdded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.TodoChecklistProjection;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.query.UnableToResolveException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcPostgresExecutedByResolverTest {

    private static ExecutedBy BOB = new ExecutedBy.ServiceAccount("bob");
    private static ExecutedBy ALICE = new ExecutedBy.ServiceAccount("alice");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(
                    TodoProjection.class, TodoChecklistProjection.class))
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Inject
    JdbcPostgresExecutedByResolver jdbcPostgresExecutedByResolver;

    @Inject
    PassphraseProvider passphraseProvider;

    @ApplicationScoped
    @Priority(1)
    @Alternative
    public static class StubPassphraseProvider implements PassphraseProvider {

        @Override
        public Passphrase provide(final OwnedBy ownedBy) {
            return PassphraseSample.PASSPHRASE_1;
        }

        @Override
        public Passphrase ban(final OwnedBy ownedBy) throws UnableToBanPassphraseException {
            throw new IllegalStateException("Should not be called");
        }
    }

    @Inject
    @BusinessMapper
    ObjectMapper objectMapper;

    public void insert(final Event<?> event, final AggregateRoot<?> aggregateRoot, final OwnedBy ownedBy, final ExecutedBy executedBy) {
        insert(List.of(event), aggregateRoot, ownedBy, executedBy);
    }

    public void insert(final List<Event<?>> events, final AggregateRoot<?> aggregateRoot, final OwnedBy ownedBy, final ExecutedBy executedBy) {
        try (final Connection connection = dataSource.getConnection()) {
            final Passphrase provided = passphraseProvider.provide(ownedBy);
            int version = 0;
            for (final Event<?> event : events) {
                final String eventPayload = objectMapper.writeValueAsString(event);
                try (final PreparedStatement preparedStatement = connection.prepareStatement(
                        // language=sql
                        """
                                INSERT INTO event (aggregate_root_id, aggregate_root_type, version, stored_at, event_type, event_payload, owned_by, belongs_to, executed_by) 
                                VALUES (?, ?, ?, ?, ?, public.pgp_sym_encrypt(?::text, ?), ?, ?, ?)
                                """)) {
                    preparedStatement.setString(1, aggregateRoot.id().id());
                    preparedStatement.setString(2, aggregateRoot.getClass().getSimpleName());
                    preparedStatement.setLong(3, version);
                    preparedStatement.setTimestamp(4, Timestamp.from(Instant.now()));
                    preparedStatement.setString(5, event.getClass().getSimpleName());
                    preparedStatement.setString(6, eventPayload);
                    preparedStatement.setString(7, new String(provided.passphrase()));
                    preparedStatement.setString(8, ownedBy.id());
                    preparedStatement.setString(9, aggregateRoot.belongsTo().id());
                    preparedStatement.setString(10, executedBy.value());
                    preparedStatement.executeUpdate();
                } catch (final SQLException e) {
                    throw new RuntimeException(e);
                }
                version++;
            }
        } catch (UnableToProvidePassphraseException | SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1)
    void shouldStoreInDatabase() {
        // Given
        {
            final Todo todo = new Todo(
                    TodoId.USER_1_TODO_1,
                    "IMPORTANT: pulse extension development",
                    Status.IN_PROGRESS,
                    true);
            insert(
                    new NewTodoCreated("IMPORTANT: pulse extension development"),
                    todo,
                    todo.ownedBy(),
                    BOB);
        }
        {
            final TodoChecklist todoChecklist = new TodoChecklist(
                    TodoChecklistId.USER_1_TODO_1_1,
                    "Implement Projection feature");
            insert(
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
            insert(
                    new NewTodoCreated("Organization vacancies"),
                    todo,
                    todo.ownedBy(),
                    BOB);
        }
        {
            final TodoChecklist todoChecklist = new TodoChecklist(
                    TodoChecklistId.USER_1_TODO_2_1,
                    "Go see family");
            insert(
                    new TodoItemAdded("Go see family"),
                    todoChecklist,
                    todoChecklist.ownedBy(),
                    BOB);
        }
        {
            final Todo todo = new Todo(
                    TodoId.USER_2_TODO_1,
                    "Bob vacancies",
                    Status.IN_PROGRESS,
                    false);
            insert(
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
        final Set<ExecutedBy> resolved = jdbcPostgresExecutedByResolver.resolve(Set.of(TodoId.USER_1_TODO_1,
                TodoChecklistId.USER_1_TODO_1_1));

        // Then
        assertThat(resolved).containsExactly(BOB, ALICE);
    }
}
