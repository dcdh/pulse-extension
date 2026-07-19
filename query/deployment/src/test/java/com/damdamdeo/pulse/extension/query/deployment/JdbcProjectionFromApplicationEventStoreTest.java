package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.event.TodoItemAdded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.OwnedByProvider;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.UnableToProvideOwnedByException;
import com.damdamdeo.pulse.extension.writer.runtime.serializer.EventTestRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.postgresql.util.PSQLException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcProjectionFromApplicationEventStoreTest {

    private static ExecutedBy BOB = new ExecutedBy.EndUser("bob", true);

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubPassphraseProvider.class,
                    TodoProjection.class, TodoChecklistProjection.class))
            .withConfigurationResource("application.properties");

    public static final class TodoProjectionSingleResultAggregateIdProjectionQuery implements SingleResultAggregateIdProjectionQuery {

        @Override
        public String query(final Passphrase passphrase, final AggregateId aggregateId) {
            // language=sql
            return """
                    WITH decrypted AS (
                      SELECT
                        aggregate_root_id,
                        aggregate_root_type,
                        public.pgp_sym_decrypt(aggregate_root_payload, '%1$s')::jsonb AS decrypted_aggregate_root_payload,
                        belongs_to
                      FROM aggregate_root
                      WHERE belongs_to = '%2$s' OR aggregate_root_id = '%2$s'
                    )
                    SELECT jsonb_build_object(
                      'todoId', d.decrypted_aggregate_root_payload -> 'id',
                      'description', d.decrypted_aggregate_root_payload ->> 'description',
                      'status', d.decrypted_aggregate_root_payload ->> 'status',
                      'important', d.decrypted_aggregate_root_payload ->> 'important',
                      'checklist', COALESCE(
                        jsonb_agg(
                          jsonb_build_object(
                            'todoChecklistId', i.decrypted_aggregate_root_payload -> 'id',
                            'description', i.decrypted_aggregate_root_payload ->> 'description'
                          )
                        ), '[]'::jsonb
                      )
                    ) AS response
                    FROM decrypted d
                    LEFT JOIN decrypted i
                      ON i.belongs_to = d.aggregate_root_id
                     AND i.aggregate_root_type = 'TodoChecklist'
                    WHERE d.aggregate_root_type = 'Todo'
                      AND d.aggregate_root_id = '%2$s'
                    GROUP BY d.aggregate_root_id, d.aggregate_root_type, d.decrypted_aggregate_root_payload, d.belongs_to;
                    """.formatted(new String(passphrase.passphrase()), aggregateId.id());
        }
    }

    public static final class TodoProjectionMultipleResultProjectionQuery implements MultipleResultProjectionQuery<SampleInput> {

        @Override
        public String query(final Passphrase passphrase, final OwnedBy ownedBy, final SampleInput input) {
            // language=sql
            return """
                    WITH decrypted AS (
                      SELECT
                        aggregate_root_id,
                        aggregate_root_type,
                        public.pgp_sym_decrypt(aggregate_root_payload, '%1$s')::jsonb AS decrypted_aggregate_root_payload,
                        belongs_to,
                        owned_by
                      FROM aggregate_root
                      WHERE owned_by = '%2$s'
                    )
                    SELECT jsonb_build_object(
                      'todoId', d.decrypted_aggregate_root_payload -> 'id',
                      'description', d.decrypted_aggregate_root_payload ->> 'description',
                      'status', d.decrypted_aggregate_root_payload ->> 'status',
                      'important', d.decrypted_aggregate_root_payload ->> 'important',
                      'checklist', COALESCE(
                        jsonb_agg(
                          jsonb_build_object(
                            'todoChecklistId', i.decrypted_aggregate_root_payload -> 'id',
                            'description', i.decrypted_aggregate_root_payload ->> 'description'
                          )
                        ), '[]'::jsonb
                      )
                    ) AS response
                    FROM decrypted d
                    LEFT JOIN decrypted i
                      ON i.belongs_to = d.aggregate_root_id
                     AND i.aggregate_root_type = 'TodoChecklist'
                    WHERE d.aggregate_root_type = 'Todo'
                      AND d.owned_by = '%2$s'
                    GROUP BY d.aggregate_root_id, d.aggregate_root_type, d.decrypted_aggregate_root_payload, d.belongs_to
                    ORDER BY d.aggregate_root_id ASC;
                    """.formatted(new String(passphrase.passphrase()), ownedBy.id());
        }
    }

    @Inject
    ProjectionFromEventStore<TodoProjection> todoProjectionProjectionFromEventStore;

    @Inject
    EventTestRepository eventTestRepository;

    @Inject
    TransactionManager transactionManager;

    @Inject
    DataSource dataSource;

    @Priority(1)
    @Dependent
    @Alternative
    public static class StubOwnedByProvider implements OwnedByProvider {

        @Override
        public OwnedBy getByAggregateId(final AggregateId aggregateId) throws UnableToProvideOwnedByException {
            Objects.requireNonNull(aggregateId);
            return OwnedBy.from(((TodoId) aggregateId).userId());
        }
    }

    @Test
    @Order(1)
    void shouldFindOneByAggregateIdReturnFoundAggregate() {
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
                    BOB);
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
        final Optional<Result<TodoProjection>> foundOneByAggregateId = todoProjectionProjectionFromEventStore.findOneByAggregateId(TodoId.USER_1_TODO_1,
                new TodoProjectionSingleResultAggregateIdProjectionQuery());

        // Then
        assertThat(foundOneByAggregateId).isEqualTo(Optional.of(
                Result.of(
                        new TodoProjection(
                                new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1),
                                "IMPORTANT: pulse extension development",
                                Status.IN_PROGRESS,
                                true,
                                List.of(
                                        new TodoChecklistProjection(
                                                new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1),
                                                "Implement Projection feature"
                                        )
                                )
                        ),
                        Set.of(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1),
                                new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1)))
        ));
    }

    @Test
    @Order(2)
    void shouldFindByAggregateIdReturnEmptyWhenNotFound() {
        // Given

        // When
        final Optional<Result<TodoProjection>> foundOneByAggregateId = todoProjectionProjectionFromEventStore.findOneByAggregateId(TodoId.USER_3_TODO_1,
                new TodoProjectionSingleResultAggregateIdProjectionQuery());

        // Then
        assertThat(foundOneByAggregateId).isEqualTo(Optional.empty());
    }

    @Test
    @Order(3)
    void shouldFindOneByAggregateIdRollbackTransactionOnPostgresSQLException() {
        // Given
        final List<Integer> status = new ArrayList<>();
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        todoProjectionProjectionFromEventStore.findOneByAggregateId(TodoId.USER_1_TODO_1,
                                new TodoProjectionSingleResultAggregateIdProjectionQuery());
                        status.add(transactionManager.getStatus());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (final SQLException e) {
                            // do nothing
                        }
                        todoProjectionProjectionFromEventStore.findOneByAggregateId(TodoId.USER_1_TODO_1,
                                new TodoProjectionSingleResultAggregateIdProjectionQuery());
                        throw new IllegalStateException("Should not reach this point");
                    })
            ));
            status.add(transactionManager.getStatus());
            return null;
        }));

        // Then
        // verify DB is empty (rollback happened)
        assertAll(
                () -> expectedException.get().isExactlyInstanceOf(ProjectionException.class)
                        .cause()
                        .isExactlyInstanceOf(PSQLException.class),
                () -> assertThat(status).containsExactly(jakarta.transaction.Status.STATUS_ACTIVE, jakarta.transaction.Status.STATUS_ACTIVE, jakarta.transaction.Status.STATUS_MARKED_ROLLBACK));
    }

    @Test
    @Order(4)
    void shouldGetByAggregateIdReturnFoundAggregate() {
        // Given

        // When
        final Result<TodoProjection> getOneByAggregateId = todoProjectionProjectionFromEventStore.getOneByAggregateId(TodoId.USER_1_TODO_1,
                new TodoProjectionSingleResultAggregateIdProjectionQuery());

        // Then
        assertThat(getOneByAggregateId).isEqualTo(Result.of(
                new TodoProjection(
                        new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1),
                        "IMPORTANT: pulse extension development",
                        Status.IN_PROGRESS,
                        true,
                        List.of(
                                new TodoChecklistProjection(
                                        new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1),
                                        "Implement Projection feature"
                                )
                        )
                ),
                Set.of(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1),
                        new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1)))
        );
    }

    @Test
    @Order(5)
    void shouldGetByAggregateIdThrowProjectionExceptionWhenNotFound() {
        // Given

        // When && Then
        assertThatThrownBy(() -> todoProjectionProjectionFromEventStore.getOneByAggregateId(TodoId.USER_3_TODO_1,
                new TodoProjectionSingleResultAggregateIdProjectionQuery()))
                .isExactlyInstanceOf(ProjectionException.class)
                .hasFieldOrPropertyWithValue("aggregateId", new TodoId(UserId.USER_3, TodoId.SEQUENCE_NUMBER_1))
                .cause()
                .isExactlyInstanceOf(UnknownProjectionException.class);
    }

    @Test
    @Order(6)
    void shouldGetOneByAggregateIdRollbackTransactionOnPostgresSQLException() {
        // Given
        final List<Integer> status = new ArrayList<>();
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        todoProjectionProjectionFromEventStore.getOneByAggregateId(TodoId.USER_1_TODO_1,
                                new TodoProjectionSingleResultAggregateIdProjectionQuery());
                        status.add(transactionManager.getStatus());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (final SQLException e) {
                            // do nothing
                        }
                        todoProjectionProjectionFromEventStore.getOneByAggregateId(TodoId.USER_1_TODO_1,
                                new TodoProjectionSingleResultAggregateIdProjectionQuery());
                        throw new IllegalStateException("Should not reach this point");
                    })
            ));
            status.add(transactionManager.getStatus());
            return null;
        }));

        // Then
        // verify DB is empty (rollback happened)
        assertAll(
                () -> expectedException.get().isExactlyInstanceOf(ProjectionException.class)
                        .cause()
                        .isExactlyInstanceOf(PSQLException.class),
                () -> assertThat(status).containsExactly(jakarta.transaction.Status.STATUS_ACTIVE, jakarta.transaction.Status.STATUS_ACTIVE, jakarta.transaction.Status.STATUS_MARKED_ROLLBACK));
    }

    @Test
    @Order(7)
    void shouldFindAllBy() {
        // Given

        // When
        final Result<TodoProjection> todos = todoProjectionProjectionFromEventStore.findAllBy(Todo.OWNED_BY_USER_1,
                new SampleInput(),
                new TodoProjectionMultipleResultProjectionQuery());

        // Then
        assertAll(
                () -> assertThat(todos.projections()).containsExactly(
                        new TodoProjection(
                                new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1),
                                "IMPORTANT: pulse extension development",
                                Status.IN_PROGRESS,
                                true,
                                List.of(
                                        new TodoChecklistProjection(
                                                new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1),
                                                "Implement Projection feature"
                                        )
                                )
                        ),
                        new TodoProjection(
                                new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_2),
                                "Organization vacancies",
                                Status.IN_PROGRESS,
                                false,
                                List.of(
                                        new TodoChecklistProjection(
                                                new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_2), TodoChecklistId.SEQUENCE_NUMBER_1),
                                                "Go see family"
                                        )
                                )
                        )
                ),
                () -> assertThat(todos.count()).isEqualTo(2),
                () -> assertThat(todos.aggregateIds()).containsExactlyInAnyOrder(
                        new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1),
                        new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1),
                        new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_2),
                        new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_2), TodoChecklistId.SEQUENCE_NUMBER_1)
                ),
                () -> assertThat(todos.getFirst()).isEqualTo(new TodoProjection(
                        new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1),
                        "IMPORTANT: pulse extension development",
                        Status.IN_PROGRESS,
                        true,
                        List.of(
                                new TodoChecklistProjection(
                                        new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1),
                                        "Implement Projection feature"
                                )
                        )
                ))
        );
    }

    @Test
    @Order(8)
    void shouldFindAllByRollbackTransactionOnPostgresSQLException() {
        // Given
        final List<Integer> status = new ArrayList<>();
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        todoProjectionProjectionFromEventStore.findAllBy(Todo.OWNED_BY_USER_1,
                                new SampleInput(),
                                new TodoProjectionMultipleResultProjectionQuery());
                        status.add(transactionManager.getStatus());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (final SQLException e) {
                            // do nothing
                        }
                        todoProjectionProjectionFromEventStore.findAllBy(Todo.OWNED_BY_USER_1,
                                new SampleInput(),
                                new TodoProjectionMultipleResultProjectionQuery());
                        throw new IllegalStateException("Should not reach this point");
                    })
            ));
            status.add(transactionManager.getStatus());
            return null;
        }));

        // Then
        // verify DB is empty (rollback happened)
        assertAll(
                () -> expectedException.get().isExactlyInstanceOf(ProjectionException.class)
                        .cause()
                        .isExactlyInstanceOf(PSQLException.class),
                () -> assertThat(status).containsExactly(jakarta.transaction.Status.STATUS_ACTIVE, jakarta.transaction.Status.STATUS_ACTIVE, jakarta.transaction.Status.STATUS_MARKED_ROLLBACK));
    }
}
