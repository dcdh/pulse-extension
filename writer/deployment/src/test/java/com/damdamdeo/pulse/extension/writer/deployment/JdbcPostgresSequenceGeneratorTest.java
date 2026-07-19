package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.writer.runtime.JdbcPostgresSequenceGenerator;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.transaction.RollbackException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionalException;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.postgresql.util.PSQLException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static jakarta.transaction.Status.STATUS_ACTIVE;
import static jakarta.transaction.Status.STATUS_MARKED_ROLLBACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcPostgresSequenceGeneratorTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @Inject
    TransactionManager transactionManager;

    @Inject
    JdbcPostgresSequenceGenerator jdbcPostgresSequenceGenerator;

    @Inject
    DataSource dataSource;

    @Inject
    StubApplicationNamingProvider stubApplicationNamingProvider;

    record SampleIdentifiable() implements Identifiable {

        @Override
        public String id() {
            throw new IllegalStateException("Should not be called");
        }
    }

    @Test
    void shouldRetrieveThrowTransactionalExceptionWhenNotExecutedInTransaction() {
        assertThatThrownBy(() -> jdbcPostgresSequenceGenerator.nextFor(TodoId.class))
                .isExactlyInstanceOf(TransactionalException.class)
                .hasMessage("ARJUNA016110: Transaction is required for invocation");
    }

    @ApplicationScoped
    @Priority(1)
    @Alternative
    public static class StubApplicationNamingProvider implements ApplicationNamingProvider {

        private final static ApplicationNaming DEFAULT = new ApplicationNaming("TodoTaking");

        private ApplicationNaming override;

        @Override
        public ApplicationNaming provide() {
            return override != null ? override : DEFAULT;
        }

        public void override(final ApplicationNaming applicationNaming) {
            this.override = Objects.requireNonNull(applicationNaming);
        }

        public void reset() {
            this.override = null;
        }
    }

    @BeforeEach
    @AfterEach
    void reset() {
        stubApplicationNamingProvider.reset();
    }

    @Order(1)
    @Test
    void shouldGenerateSequenceInOrder() {
        // Given
        final Class<? extends AggregateId> given = TodoId.class;

        // When && Then
        assertAll(
                () -> assertThat(QuarkusTransaction.requiringNew().call(() -> jdbcPostgresSequenceGenerator.nextFor(given)))
                        .isEqualTo(SequenceNumber.fromNumber(1L)),
                () -> assertThat(QuarkusTransaction.requiringNew().call(() -> jdbcPostgresSequenceGenerator.nextFor(given)))
                        .isEqualTo(SequenceNumber.fromNumber(2L)),
                () -> assertThat(listSequences()).containsExactly("TodoTaking|TodoId|himself|2")
        );
    }

    @Order(2)
    @Test
    void shouldGenerateSequenceInOrderFromBelongsTo() {
        // Given
        final List<For<TodoChecklistId>> given = List.of(
                new For<>(TodoChecklistId.class, TodoChecklist.BELONGS_TO_USER_1_TODO_1),
                new For<>(TodoChecklistId.class, TodoChecklist.BELONGS_TO_USER_1_TODO_1),
                new For<>(TodoChecklistId.class, TodoChecklist.BELONGS_TO_USER_2_TODO_1));

        // When
        final List<SequenceNumber> sequenceNumbers = new ArrayList<>();
        for (final For<TodoChecklistId> forTodoChecklistId : given) {
            final SequenceNumber sequenceNumber = QuarkusTransaction.requiringNew().call(() -> jdbcPostgresSequenceGenerator.nextFor(forTodoChecklistId));
            sequenceNumbers.add(sequenceNumber);
        }

        // Then
        final List<String> sequences = listSequences();

        assertAll(
                () -> assertThat(sequenceNumbers).containsExactly(
                        SequenceNumber.fromNumber(1L),
                        SequenceNumber.fromNumber(2L),
                        SequenceNumber.fromNumber(1L)),
                () -> assertThat(sequences).containsExactly(
                        "TodoTaking|TodoChecklistId|U000001-T000001|2",
                        "TodoTaking|TodoChecklistId|U000002-T000001|1",
                        "TodoTaking|TodoId|himself|2"));
    }

    @Order(3)
    @Test
    void shouldRollbackTransactionOnChangingOwnerWhenAggregateIdNextFor() {
        // Given
        final List<Integer> status = new ArrayList<>();
        final Class<? extends AggregateId> given = TodoId.class;
        stubApplicationNamingProvider.override(new ApplicationNaming("TodoRegistered"));

        // When && Then
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            assertThatThrownBy(() -> QuarkusTransaction.joiningExisting().call(() -> jdbcPostgresSequenceGenerator.nextFor(given)))
                    .isExactlyInstanceOf(QuarkusTransactionException.class)
                    .cause()
                    .isExactlyInstanceOf(SequenceGenerationException.class)
                    .cause()
                    .isExactlyInstanceOf(PSQLException.class)
                    .hasMessageStartingWith("ERROR: Sequence already owned by TodoTaking");
            status.add(transactionManager.getStatus());
            return null;
        }))
                .isExactlyInstanceOf(QuarkusTransactionException.class)
                .cause()
                .isExactlyInstanceOf(RollbackException.class);
        assertThat(status).containsExactly(STATUS_ACTIVE, STATUS_MARKED_ROLLBACK);
    }

    @Order(4)
    @Test
    void shouldRollbackTransactionOnChangingOwnerWhenForNextFor() {
        // Given
        final List<Integer> status = new ArrayList<>();
        final For<TodoChecklistId> given = new For<>(TodoChecklistId.class, TodoChecklist.BELONGS_TO_USER_2_TODO_1);
        stubApplicationNamingProvider.override(new ApplicationNaming("TodoRegistered"));

        // When && Then
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            assertThatThrownBy(() -> QuarkusTransaction.joiningExisting().call(() -> jdbcPostgresSequenceGenerator.nextFor(given)))
                    .isExactlyInstanceOf(QuarkusTransactionException.class)
                    .cause()
                    .isExactlyInstanceOf(SequenceGenerationException.class)
                    .cause()
                    .isExactlyInstanceOf(PSQLException.class)
                    .hasMessageStartingWith("ERROR: Sequence already owned by TodoTaking");
            status.add(transactionManager.getStatus());
            return null;
        }))
                .isExactlyInstanceOf(QuarkusTransactionException.class)
                .cause()
                .isExactlyInstanceOf(RollbackException.class);
        assertThat(status).containsExactly(STATUS_ACTIVE, STATUS_MARKED_ROLLBACK);
    }

    @Order(5)
    @Test
    void shouldFailWhenIncrementNotByOne() {
        // Given

        // When && Then
        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE pulse.sequences SET next_value = 20
                                 WHERE owned_by = 'TodoTaking' AND identifiable_clazz = 'TodoId' AND belongs_to = 'himself';
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageStartingWith("ERROR: next_value must be incremented by exactly 1 (old=2, new=20)");
    }

    @Order(6)
    @Test
    void shouldFailWhenDeletingASequence() {
        // Given

        // When && Then
        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 DELETE FROM pulse.sequences
                                 WHERE owned_by = 'TodoTaking' AND identifiable_clazz = 'TodoId' AND belongs_to = 'himself';
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageStartingWith("ERROR: Deletion of sequences is not allowed (identifiable_clazz=TodoId, belongs_to=himself).");
    }

    @Test
    void shouldRollbackTransactionWhenUnableToRetrievePassphraseExceptionIsThrown() {
        // Given
        final List<Integer> status = new ArrayList<>();
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        jdbcPostgresSequenceGenerator.nextFor(new For<>(TodoChecklistId.class, TodoChecklist.BELONGS_TO_USER_1_TODO_1));
                        status.add(transactionManager.getStatus());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (final SQLException e) {
                            // do nothing
                        }
                        jdbcPostgresSequenceGenerator.nextFor(new For<>(TodoChecklistId.class, TodoChecklist.BELONGS_TO_USER_1_TODO_1));
                        throw new IllegalStateException("Should not reach this point");
                    })
            ));
            status.add(transactionManager.getStatus());
            return null;
        })).isExactlyInstanceOf(QuarkusTransactionException.class)
                .cause()
                .isExactlyInstanceOf(RollbackException.class);

        // Then
        assertAll(
                () -> expectedException.get().isExactlyInstanceOf(QuarkusTransactionException.class)
                        .hasCauseInstanceOf(SequenceGenerationException.class),
                () -> assertThat(status).containsExactly(STATUS_ACTIVE, STATUS_ACTIVE, STATUS_MARKED_ROLLBACK)
        );
    }

    public List<String> listSequences() {
        final List<String> sequences = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT owned_by, identifiable_clazz, belongs_to, next_value
                                 FROM pulse.sequences
                                 ORDER BY identifiable_clazz, belongs_to;
                             """);
             final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                final String ownedBy = rs.getString("owned_by");
                final String identifiableClazz = rs.getString("identifiable_clazz");
                final String belongsTo = rs.getString("belongs_to");
                final int nextValue = rs.getInt("next_value");
                sequences.add(ownedBy + "|" + identifiableClazz + "|" + belongsTo + "|" + nextValue);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        return sequences;
    }
}
