package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.writer.runtime.JdbcPostgresSequenceGenerator;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionalException;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

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

    @Test
    void shouldCreateSequences() {
        // Given
        final Class<? extends AggregateId> given = TodoId.class;

        // When
        final SequenceNumber sequenceNumber = QuarkusTransaction.requiringNew().call(() -> jdbcPostgresSequenceGenerator.nextFor(given));

        // Then
        final List<String> sequences = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT sequence_schema, sequence_name
                                 FROM information_schema.sequences
                                 ORDER BY sequence_schema, sequence_name;
                             """);
             final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                final String schema = rs.getString("sequence_schema");
                final String sequence = rs.getString("sequence_name");
                sequences.add(schema + "." + sequence);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        assertAll(
                () -> assertThat(sequences).containsExactly("todo_taking.seq_customfailingtodoid",
                        "todo_taking.seq_customidentifiable",
                        "todo_taking.seq_sampleidentifiable",
                        "todo_taking.seq_todochecklistid",
                        "todo_taking.seq_todoid",
                        "todo_taking.seq_userid"),
                () -> assertThat(sequenceNumber).isEqualTo(SequenceNumber.fromNumber(1L)));
    }

    @Test
    void shouldGenerateSequenceInOrder() {
        // Given
        final Class<? extends AggregateId> given = TodoChecklistId.class;

        // When && Then
        assertAll(
                () -> assertThat(QuarkusTransaction.requiringNew().call(() -> jdbcPostgresSequenceGenerator.nextFor(given)))
                        .isEqualTo(SequenceNumber.fromNumber(1L)),
                () -> assertThat(QuarkusTransaction.requiringNew().call(() -> jdbcPostgresSequenceGenerator.nextFor(given)))
                        .isEqualTo(SequenceNumber.fromNumber(2L))
        );
    }

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

        final List<String> sequences = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT identifiable_clazz, belongs_to, next_value
                                 FROM sequence_by_identifiable_clazz_and_belongs_to
                                 ORDER BY identifiable_clazz, belongs_to;
                             """);
             final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                final String identifiableClazz = rs.getString("identifiable_clazz");
                final String ownedBy = rs.getString("belongs_to");
                final int nextValue = rs.getInt("next_value");
                sequences.add(identifiableClazz + "|" + ownedBy + "|" + nextValue);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        assertAll(
                () -> assertThat(sequenceNumbers).containsExactly(
                        SequenceNumber.fromNumber(1L),
                        SequenceNumber.fromNumber(2L),
                        SequenceNumber.fromNumber(1L)),
                () -> assertThat(sequences).containsExactly(
                        "TodoChecklistId|U000001-T000001|2", "TodoChecklistId|U000002-T000001|1"));
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
                        } catch (SQLException e) {
                            // do nothing
                        }
                        jdbcPostgresSequenceGenerator.nextFor(new For<>(TodoChecklistId.class, TodoChecklist.BELONGS_TO_USER_1_TODO_1));
                        throw new IllegalStateException("Should not reach this point");
                    })
            ));
            status.add(transactionManager.getStatus());
            return null;
        }));

        // Then
        assertAll(
                () -> expectedException.get().isExactlyInstanceOf(QuarkusTransactionException.class)
                        .hasCauseInstanceOf(SequenceGenerationException.class),
                () -> assertThat(status).containsExactly(jakarta.transaction.Status.STATUS_ACTIVE, jakarta.transaction.Status.STATUS_ACTIVE, Status.STATUS_MARKED_ROLLBACK)
        );
    }
}
