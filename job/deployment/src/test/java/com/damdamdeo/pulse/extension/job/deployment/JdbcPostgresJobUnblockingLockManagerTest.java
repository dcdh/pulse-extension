package com.damdamdeo.pulse.extension.job.deployment;

import com.damdamdeo.pulse.extension.core.job.JobExecutionException;
import com.damdamdeo.pulse.extension.core.job.JobExecutor;
import com.damdamdeo.pulse.extension.core.job.JobLockingException;
import com.damdamdeo.pulse.extension.core.job.JobName;
import com.damdamdeo.pulse.extension.core.usecase.LockingException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.job.runtime.JdbcPostgresJobUnblockingLockManager;
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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

class JdbcPostgresJobUnblockingLockManagerTest {

    private final static Logger LOGGER = Logger.getLogger(JdbcPostgresJobUnblockingLockManagerTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresJobUnblockingLockManager jdbcPostgresJobUnblockingLockManager;

    @Test
    void shouldLockUnblockingOnlyOnce() throws LockingException, UseCaseException {
        // Given
        final JobExecutor jobExecutor = spy(new JobExecutor() {

            @Override
            public void execute() throws JobExecutionException {
                try {
                    LOGGER.info("Sleeping for 5 seconds");
                    TimeUnit.SECONDS.sleep(5);
                    LOGGER.info("Done sleeping");
                } catch (final InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
            }
        });

        // When
        final Instant start = Instant.now();
        final List<CompletableFuture<Void>> futures = IntStream.range(0, 50)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        jdbcPostgresJobUnblockingLockManager.executeWithUnblockingLock(
                                new JobName("SampleJob"), jobExecutor);
                    } catch (final JobLockingException | JobExecutionException exception) {
                        throw new CompletionException(exception);
                    }
                }))
                .toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        // Then
        final Duration between = Duration.between(start, Instant.now());
        assertAll(
                () -> assertThat(between).isStrictlyBetween(Duration.ofSeconds(5), Duration.ofSeconds(8)),
                () -> verify(jobExecutor, times(1)).execute(),
                () -> assertThat(getLocks()).isEmpty()
        );
    }

    @Test
    void shouldThrowJobExecutionExceptionReleaseLock() throws LockingException, UseCaseException {
        // Given
        final List<String> locksInExecute = new ArrayList<>();

        final JobExecutor jobExecutor = spy(new JobExecutor() {

            @Override
            public void execute() throws JobExecutionException {
                locksInExecute.addAll(getLocks());
                throw new JobExecutionException(new RuntimeException("Something went wrong"));
            }
        });

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> jdbcPostgresJobUnblockingLockManager.executeWithUnblockingLock(
                        new JobName("SampleJob"), jobExecutor))
                        .isExactlyInstanceOf(JobExecutionException.class)
                        .cause()
                        .isExactlyInstanceOf(RuntimeException.class)
                        .hasMessage("Something went wrong"),
                () -> assertThat(locksInExecute).containsExactly("ExclusiveLock|t|4294967295|3116955027"),
                () -> assertThat(getLocks()).isEmpty());
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
