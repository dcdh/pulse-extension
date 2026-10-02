package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.Handled;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.usecase.LockingException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseExceptionCode;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseExecutor;
import com.damdamdeo.pulse.extension.writer.runtime.JdbcPostgresDistributedLockManager;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcPostgresDistributedLockManagerTest {

    private final static Logger LOGGER = Logger.getLogger(JdbcPostgresDistributedLockManagerTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresDistributedLockManager jdbcPostgresDistributedLockManager;

    @Test
    void shouldLock() throws LockingException, UseCaseException {
        // Given
        final UseCaseExecutor<TodoId, MarkTodoAsDone, Todo> useCaseExecutor = new UseCaseExecutor<TodoId, MarkTodoAsDone, Todo>() {

            @Override
            public Handled<Todo, TodoId> execute(final MarkTodoAsDone command) throws UseCaseException {
                try {
                    LOGGER.info("Sleeping for 5 seconds");
                    TimeUnit.SECONDS.sleep(5);
                    LOGGER.info("Done sleeping");
                } catch (final InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
                return null;
            }
        };

        // When
        final Instant start = Instant.now();
        final List<CompletableFuture<Void>> futures = IntStream.range(0, 3)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        jdbcPostgresDistributedLockManager.executeWithLock(
                                new MarkTodoAsDone(TodoId.USER_1_TODO_1),
                                useCaseExecutor
                        );
                    } catch (final UseCaseException | LockingException exception) {
                        throw new CompletionException(exception);
                    }
                }))
                .toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        // Then
        final Duration between = Duration.between(start, Instant.now());
        assertThat(between).isGreaterThanOrEqualTo(Duration.ofSeconds(15));
    }

    @Test
    void shouldThrowUseCaseExceptionReleaseLock() throws LockingException, UseCaseException {
        // Given
        final List<String> locksInExecute = new ArrayList<>();

        final UseCaseExecutor<TodoId, MarkTodoAsDone, Todo> useCaseExecutor = new UseCaseExecutor<TodoId, MarkTodoAsDone, Todo>() {

            @Override
            public Handled<Todo, TodoId> execute(final MarkTodoAsDone command) throws UseCaseException {
                locksInExecute.addAll(getLocks());
                throw new UseCaseException(new RuntimeException("Something went wrong"), UseCaseExceptionCode.INFRASTRUCTURE_FAILURE);
            }
        };

        // When / Then
        assertThatThrownBy(() -> jdbcPostgresDistributedLockManager.executeWithLock(new MarkTodoAsDone(TodoId.USER_1_TODO_1), useCaseExecutor))
                .isExactlyInstanceOf(UseCaseException.class)
                .cause()
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Something went wrong");
        assertThat(locksInExecute).containsExactly("ExclusiveLock|t|4294967295|2490515488");
        assertThat(getLocks()).isEmpty();
    }

    private List<String> getLocks() {
        final List<String> locks = new ArrayList<>();
        try (final InstanceHandle<DataSource> instance = Arc.container().instance(DataSource.class)) {
            final DataSource dataSource = instance.get();
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement("SELECT pid, mode, granted, classid, objid FROM pg_locks WHERE locktype = 'advisory'");
                 final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    locks.add(String.join("|", rs.getString("mode"), rs.getString("granted"),
                            rs.getString("classid"), rs.getString("objid")));
                }
            } catch (final SQLException exception) {
                throw new RuntimeException(exception);
            }
        }
        return locks;
    }
}
