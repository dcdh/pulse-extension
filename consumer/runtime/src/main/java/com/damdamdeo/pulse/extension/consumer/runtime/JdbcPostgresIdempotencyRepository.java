package com.damdamdeo.pulse.extension.consumer.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.*;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.jboss.logmanager.Level;

import javax.sql.DataSource;
import java.sql.*;
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
    public Optional<LastConsumedAggregateVersion> findLastAggregateVersionBy(final Target target, final FromApplication fromApplication,
                                                                             final AggregateRootType aggregateRootType, final AggregateId aggregateId) throws IdempotencyException {
        Objects.requireNonNull(target);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(aggregateId);
        final String sql = """
                    SELECT last_consumed_version
                    FROM t_idempotency
                    WHERE target = ?
                      AND from_application = ?
                      AND aggregate_root_type = ?
                      AND aggregate_root_id = ?
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.name());
            ps.setString(2, fromApplication.value());
            ps.setString(3, aggregateRootType.type());
            ps.setString(4, aggregateId.id());
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
    public void upsert(final Target target, final FromApplication fromApplication, final EventKey eventKey) throws IdempotencyException {
        Objects.requireNonNull(target);
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(eventKey);
        final AggregateRootType aggregateRootType = Objects.requireNonNull(eventKey.toAggregateRootType());
        final AggregateId aggregateRootId = Objects.requireNonNull(eventKey.toAggregateId());
        final CurrentVersionInConsumption currentVersionInConsumption = Objects.requireNonNull(eventKey.toCurrentVersionInConsumption());
        // language=sql
        final String sql = """
                    INSERT INTO t_idempotency (target, from_application, aggregate_root_type, aggregate_root_id, last_consumed_version)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (target, from_application, aggregate_root_type, aggregate_root_id)
                    DO UPDATE SET last_consumed_version = EXCLUDED.last_consumed_version
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, target.name());
            ps.setString(2, fromApplication.value());
            ps.setString(3, aggregateRootType.type());
            ps.setString(4, aggregateRootId.id());
            ps.setInt(5, currentVersionInConsumption.version());
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new IdempotencyException("Failed to upsert idempotency entry", e);
        }
    }

    void onStartup(final @Observes @Priority(30) StartupEvent event) {
        // language=sql
        final String ddl = """
                    CREATE TABLE IF NOT EXISTS t_idempotency (
                        target character varying(255) not null,
                        from_application character varying(255) not null,
                        aggregate_root_type character varying(255) not null,
                        aggregate_root_id character varying(255) not null,
                        last_consumed_version bigint not null,
                        PRIMARY KEY (target, from_application, aggregate_root_type, aggregate_root_id)
                    )
                """;
        try (final Connection connection = dataSource.getConnection();
             final Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(ddl);
            LOGGER.info("Table t_idempotency is ready.");
        } catch (final SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize t_idempotency table", e);
            throw new IllegalStateException("Cannot initialize t_idempotency table", e);
        }
    }
}
