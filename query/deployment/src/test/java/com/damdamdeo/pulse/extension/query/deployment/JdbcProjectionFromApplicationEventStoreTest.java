package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.event.TodoItemAdded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.writer.runtime.serializer.EventTestRepository;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcProjectionFromApplicationEventStoreTest {

    private static ExecutedBy BOB = new ExecutedBy.EndUser("bob", true);

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubPassphraseProvider.class,
                    TodoProjection.class, TodoChecklistProjection.class))
            .withConfigurationResource("application.properties");

    public static final class TodoProjectionSingleResultAggregateQuery implements SingleResultAggregateQuery<SampleInput> {

        @Override
        public String query(final Passphrase passphrase, final AggregateId aggregateId, final SampleInput input) {
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

    public static final class TodoProjectionMultipleResultAggregateQuery implements MultipleResultAggregateQuery<SampleInput> {

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
    ProjectionFromEventStore<SampleInput, TodoProjection> todoProjectionProjectionFromEventStore;

    @Inject
    EventTestRepository eventTestRepository;

    @Test
    @Order(1)
    void shouldFindBy() {
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
        final Optional<Result<TodoProjection>> foundBy = todoProjectionProjectionFromEventStore.findBy(Todo.OWNED_BY_USER_1, new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1),
                new SampleInput(), new TodoProjectionSingleResultAggregateQuery());

        // Then
        assertThat(foundBy).isEqualTo(Optional.of(
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
    void shouldListAll() {
        // Given

        // When
        final Result<TodoProjection> todos = todoProjectionProjectionFromEventStore.findAll(Todo.OWNED_BY_USER_1,
                new SampleInput(),
                new TodoProjectionMultipleResultAggregateQuery());

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
}
