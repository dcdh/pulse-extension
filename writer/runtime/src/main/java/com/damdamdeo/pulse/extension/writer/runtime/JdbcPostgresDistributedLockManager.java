package com.damdamdeo.pulse.extension.writer.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.command.Handled;
import com.damdamdeo.pulse.extension.core.usecase.DistributedLockManager;
import com.damdamdeo.pulse.extension.core.usecase.LockingException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseExecutor;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class JdbcPostgresDistributedLockManager implements DistributedLockManager {

    @Inject
    Provider<DataSource> dataSource;

    @Override
    public <K extends AggregateId, C extends Command<K>, A extends AggregateRoot<K>> Handled<A, K> executeWithLock(
            final C command, final UseCaseExecutor<K, C, A> useCaseExecutor) throws LockingException, UseCaseException {
        final int lockKey = command.id().hashCode();
        try (final Connection connection = dataSource.get().getConnection()) {
            acquireLock(connection, lockKey);
            try {
                return useCaseExecutor.execute(command);
            } finally {
                releaseLock(connection, lockKey);
            }
        } catch (final SQLException exception) {
            throw new LockingException(exception);
        }
    }

    private void acquireLock(final Connection connection, final Integer key) throws SQLException {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(key);
        try (final PreparedStatement ps = connection.prepareStatement("SELECT pg_advisory_lock(?)")) {
            ps.setLong(1, key);
            ps.executeQuery();
        }
    }

    private void releaseLock(final Connection connection, final Integer key) throws SQLException {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(key);
        try (final PreparedStatement ps = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            ps.setLong(1, key);
            ps.execute();
        }
    }
}
