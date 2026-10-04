package com.damdamdeo.pulse.extension.job.runtime;

import com.damdamdeo.pulse.extension.core.job.*;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class JdbcPostgresJobUnblockingLockManager implements JobUnblockingLockManager {

    @Inject
    Provider<DataSource> dataSource;

    @Override
    public void executeWithUnblockingLock(final JobName jobName, final JobExecutor jobExecutor) throws JobLockingException, JobExecutionException {
        Objects.requireNonNull(jobName);
        Objects.requireNonNull(jobExecutor);
        final int lockKey = jobName.name().hashCode();
        try (final Connection connection = dataSource.get().getConnection()) {
            if (!tryAcquireLock(connection, lockKey)) {
                // already locked
                return;
            }
            try {
                jobExecutor.execute();
            } finally {
                releaseLock(connection, lockKey);
            }
        } catch (final SQLException exception) {
            throw new JobLockingException(exception);
        }
    }

    private boolean tryAcquireLock(final Connection connection, final Integer key) throws SQLException {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(key);
        try (final PreparedStatement ps = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            ps.setLong(1, key);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    private void releaseLock(final Connection connection, final Integer key) throws SQLException {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(key);
        try (final PreparedStatement ps = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            ps.setLong(1, key);
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next() && !rs.getBoolean(1)) {
                    throw new SQLException("Advisory lock was not held");
                }
            }
        }
    }
}
