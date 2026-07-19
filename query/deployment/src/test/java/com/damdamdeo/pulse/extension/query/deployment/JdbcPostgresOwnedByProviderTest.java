package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.JdbcPostgresOwnedByProvider;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.UnableToProvideOwnedByException;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.UnknownOwnedBy;
import com.damdamdeo.pulse.extension.writer.runtime.serializer.EventTestRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.quarkus.test.QuarkusUnitTest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcPostgresOwnedByProviderTest {

    public static ExecutedBy BOB = new ExecutedBy.EndUser("bob", true);

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubPassphraseProvider.class))
            .withConfigurationResource("application.properties");

    @Inject
    EventTestRepository eventTestRepository;

    @Inject
    JdbcPostgresOwnedByProvider jdbcPostgresOwnedByProvider;

    @Inject
    TransactionManager transactionManager;

    @Inject
    DataSource dataSource;

    @Test
    @Order(1)
    void shouldReturnOwnedByByAggregateId() throws UnableToProvideOwnedByException {
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

        // When
        final OwnedBy byAggregateId = jdbcPostgresOwnedByProvider.getByAggregateId(TodoId.USER_1_TODO_1);

        // Then
        assertThat(byAggregateId).isEqualTo(Todo.OWNED_BY_USER_1);
    }

    @Test
    @Order(2)
    void shouldThrowUnknownOwnedByWhenAggregateIdDoesNotExists() {
        // Given

        // When && Then
        assertThatThrownBy(() -> jdbcPostgresOwnedByProvider.getByAggregateId(TodoId.USER_2_TODO_1))
                .isExactlyInstanceOf(UnableToProvideOwnedByException.class)
                .cause()
                .isExactlyInstanceOf(UnknownOwnedBy.class);
    }

    @Test
    @Order(3)
    void shouldRollbackTransactionOnPostgresSQLException() {
        // Given
        final List<Integer> status = new ArrayList<>();
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        jdbcPostgresOwnedByProvider.getByAggregateId(TodoId.USER_1_TODO_1);
                        status.add(transactionManager.getStatus());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (final SQLException e) {
                            // do nothing
                        }
                        jdbcPostgresOwnedByProvider.getByAggregateId(TodoId.USER_1_TODO_1);
                        throw new IllegalStateException("Should not reach this point");
                    })
            ));
            status.add(transactionManager.getStatus());
            return null;
        }));

        // Then
        // verify DB is empty (rollback happened)
        assertAll(
                () -> expectedException.get().isExactlyInstanceOf(QuarkusTransactionException.class)
                        .cause()
                        .isExactlyInstanceOf(UnableToProvideOwnedByException.class)
                        .cause()
                        .isExactlyInstanceOf(PSQLException.class),
                () -> assertThat(status).containsExactly(jakarta.transaction.Status.STATUS_ACTIVE, jakarta.transaction.Status.STATUS_ACTIVE, jakarta.transaction.Status.STATUS_MARKED_ROLLBACK));
    }
}
