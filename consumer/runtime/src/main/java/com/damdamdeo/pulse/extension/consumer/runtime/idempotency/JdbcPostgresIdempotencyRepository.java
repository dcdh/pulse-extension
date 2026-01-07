package com.damdamdeo.pulse.extension.consumer.runtime.idempotency;

import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.LastConsumedAggregateVersion;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.*;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

@Singleton
@Unremovable
@DefaultBean
public final class JdbcPostgresIdempotencyRepository implements IdempotencyRepository {

    static final Logger LOGGER = Logger.getLogger(JdbcPostgresIdempotencyRepository.class.getName());

    private final DataSource dataSource;

    public JdbcPostgresIdempotencyRepository(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public Optional<LastConsumedAggregateVersion> findLastAggregateVersionBy(final IdempotencyKey idempotencyKey) throws IdempotencyException {
        Objects.requireNonNull(idempotencyKey);
        final String sql = """
                    SELECT last_consumed_version
                    FROM idempotency
                    WHERE purpose = ?
                      AND from_application = ?
                      AND topic = ?
                      AND aggregate_root_type = ?
                      AND aggregate_root_id = ?
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, idempotencyKey.purpose().name());
            ps.setString(2, idempotencyKey.fromApplication().value());
            ps.setString(3, idempotencyKey.topic().name());
            ps.setString(4, idempotencyKey.aggregateRootType().type());
            ps.setString(5, idempotencyKey.aggregateId().id());
            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final int version = rs.getInt("last_consumed_version");
                    return Optional.of(new LastConsumedAggregateVersion(version));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IdempotencyException("Failed to find idempotency entry", e);
        }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    @Override
    public void upsert(final IdempotencyKey idempotencyKey, final CurrentVersionInConsumption currentVersionInConsumption) throws IdempotencyException {
        Objects.requireNonNull(idempotencyKey);
        Objects.requireNonNull(currentVersionInConsumption);
        // language=sql
        final String sql = """
                    INSERT INTO idempotency (purpose, from_application, topic, aggregate_root_type, aggregate_root_id, last_consumed_version)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (purpose, from_application, topic, aggregate_root_type, aggregate_root_id)
                    DO UPDATE SET last_consumed_version = EXCLUDED.last_consumed_version
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, idempotencyKey.purpose().name());
            ps.setString(2, idempotencyKey.fromApplication().value());
            ps.setString(3, idempotencyKey.topic().name());
            ps.setString(4, idempotencyKey.aggregateRootType().type());
            ps.setString(5, idempotencyKey.aggregateId().id());
            ps.setInt(6, currentVersionInConsumption.version());
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new IdempotencyException("Failed to upsert idempotency entry", e);
        }
    }
}
