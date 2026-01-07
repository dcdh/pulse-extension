package com.damdamdeo.pulse.extension.writer.deployment.projection;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.projection.MultipleResultAggregateQuery;
import com.damdamdeo.pulse.extension.core.projection.Projection;
import com.damdamdeo.pulse.extension.core.projection.ProjectionFromEventStore;
import com.damdamdeo.pulse.extension.core.projection.SingleResultAggregateQuery;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcProjectionFromApplicationEventStoreTest {

    private static ExecutedBy BOB = new ExecutedBy.EndUser("bob", true);

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .withConfigurationResource("application.properties");

    record TodoProjection(TodoId id,
                          String description,
                          Status status,
                          boolean important,
                          List<TodoChecklistProjection> checklist) implements Projection {
    }

    record TodoChecklistProjection(TodoChecklistId id, String description) {
    }

    public static final class TodoProjectionSingleResultAggregateQuery implements SingleResultAggregateQuery {

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
                      WHERE belongs_to = '%2$s'
                    )
                    SELECT jsonb_build_object(
                      'id', d.decrypted_aggregate_root_payload -> 'id',
                      'description', d.decrypted_aggregate_root_payload ->> 'description',
                      'status', d.decrypted_aggregate_root_payload ->> 'status',
                      'important', d.decrypted_aggregate_root_payload ->> 'important',
                      'checklist', COALESCE(
                        jsonb_agg(
                          i.decrypted_aggregate_root_payload::jsonb
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

    public static final class TodoProjectionMultipleResultAggregateQuery implements MultipleResultAggregateQuery {

        @Override
        public String query(final Passphrase passphrase, final OwnedBy ownedBy) {
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
                      'id', d.decrypted_aggregate_root_payload -> 'id',
                      'description', d.decrypted_aggregate_root_payload ->> 'description',
                      'status', d.decrypted_aggregate_root_payload ->> 'status',
                      'important', d.decrypted_aggregate_root_payload ->> 'important',
                      'checklist', COALESCE(
                        jsonb_agg(
                          i.decrypted_aggregate_root_payload::jsonb
                        ), '[]'::jsonb
                      )
                    ) AS response
                    FROM decrypted d
                    LEFT JOIN decrypted i
                      ON i.belongs_to = d.aggregate_root_id
                     AND i.aggregate_root_type = 'TodoChecklist'
                    WHERE d.aggregate_root_type = 'Todo'
                      AND d.owned_by = '%2$s'
                    GROUP BY d.aggregate_root_id, d.aggregate_root_type, d.decrypted_aggregate_root_payload, d.belongs_to;
                    """.formatted(new String(passphrase.passphrase()), ownedBy.id());
        }
    }

    @Inject
    EventRepository<Todo, TodoId> todoEventRepository;

    @Inject
    EventRepository<TodoChecklist, TodoChecklistId> todoChecklistEventRepository;

    @Inject
    ProjectionFromEventStore<TodoProjection> todoProjectionProjectionFromEventStore;

    @ApplicationScoped
    static class StubPassphraseProvider implements PassphraseProvider {

        @Override
        public Passphrase provide(final OwnedBy ownedBy) {
            return PassphraseSample.PASSPHRASE;
        }
    }

    @Test
    @Order(1)
    void shouldFindBy() {
        // Given
        todoEventRepository.save(List.of(
                        new VersionizedEvent(new AggregateVersion(0),
                                new NewTodoCreated("IMPORTANT: pulse extension development"))),
                new Todo(
                        new TodoId("Damien", 1L),
                        "IMPORTANT: pulse extension development",
                        Status.IN_PROGRESS,
                        true
                ), BOB);
        todoChecklistEventRepository.save(List.of(
                        new VersionizedEvent(new AggregateVersion(0),
                                new TodoItemAdded("Implement Projection feature"))),
                new TodoChecklist(
                        new TodoChecklistId(new TodoId("Damien", 1L), 0L),
                        "Implement Projection feature"
                ), BOB);
        todoEventRepository.save(List.of(
                        new VersionizedEvent(new AggregateVersion(0),
                                new NewTodoCreated("Organization vacancies"))),
                new Todo(
                        new TodoId("Damien", 2L),
                        "Organization vacancies",
                        Status.IN_PROGRESS,
                        false
                ), BOB);
        todoChecklistEventRepository.save(List.of(
                        new VersionizedEvent(new AggregateVersion(0),
                                new TodoItemAdded("Go see family"))),
                new TodoChecklist(
                        new TodoChecklistId(new TodoId("Damien", 2L), 0L),
                        "Go see family"
                ), BOB);
        todoEventRepository.save(List.of(
                        new VersionizedEvent(new AggregateVersion(0),
                                new NewTodoCreated("Bob vacancies"))),
                new Todo(
                        new TodoId("Bob", 0L),
                        "Bob vacancies",
                        Status.IN_PROGRESS,
                        false
                ), BOB);

        // When
        final Optional<TodoProjection> foundBy = todoProjectionProjectionFromEventStore.findBy(new OwnedBy("Damien"), new TodoId("Damien", 1L), new TodoProjectionSingleResultAggregateQuery());

        // Then
        assertThat(foundBy).isEqualTo(Optional.of(
                new TodoProjection(
                        new TodoId("Damien", 1L),
                        "IMPORTANT: pulse extension development",
                        Status.IN_PROGRESS,
                        true,
                        List.of(
                                new TodoChecklistProjection(
                                        new TodoChecklistId(new TodoId("Damien", 1L), 0L),
                                        "Implement Projection feature"
                                )
                        )
                )
        ));
    }

    @Test
    @Order(2)
    void shouldListAll() {
        // Given

        // When
        List<TodoProjection> todos = todoProjectionProjectionFromEventStore.findAll(new OwnedBy("Damien"),
                new TodoProjectionMultipleResultAggregateQuery());

        // Then
        assertThat(todos).containsExactly(
                new TodoProjection(
                        new TodoId("Damien", 1L),
                        "IMPORTANT: pulse extension development",
                        Status.IN_PROGRESS,
                        true,
                        List.of(
                                new TodoChecklistProjection(
                                        new TodoChecklistId(new TodoId("Damien", 1L), 0L),
                                        "Implement Projection feature"
                                )
                        )
                ),
                new TodoProjection(
                        new TodoId("Damien", 2L),
                        "Organization vacancies",
                        Status.IN_PROGRESS,
                        false,
                        List.of(
                                new TodoChecklistProjection(
                                        new TodoChecklistId(new TodoId("Damien", 2L), 0L),
                                        "Go see family"
                                )
                        )
                )
        );
    }
}
