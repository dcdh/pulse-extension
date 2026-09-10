package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.*;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class JdbcPostgresEncodedInvolvedRepository implements EncodedInvolvedRepository {

    private final DataSource dataSource;

    public JdbcPostgresEncodedInvolvedRepository(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public Page<EncodedInvolved> findBy(final AnyAggregateId aggregateId, final Pagination pagination) throws TraceRepositoryException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(pagination);
        try (final Connection connection = dataSource.getConnection();
             // language=sql
             final PreparedStatement countPreparedStatement = connection.prepareStatement("""
                     SELECT COUNT(*) AS count FROM pulse.traceability_aggregate WHERE aggregate_root_id = ? AND aggregate_root_type = ?
                     """);
             // language=sql
             final PreparedStatement selectPreparedStatement = connection.prepareStatement("""
                     SELECT executed_by_hashed, executed_by_encoded FROM pulse.traceability_aggregate WHERE aggregate_root_id = ? AND aggregate_root_type = ? LIMIT ? OFFSET ?
                     """)) {
            if (pagination.loadAll()) {
                selectPreparedStatement.setString(1, aggregateId.id());
                final List<EncodedInvolved> content = new ArrayList<>();
                try (final ResultSet select = selectPreparedStatement.executeQuery()) {
                    while (select.next()) {
                        content.add(new EncodedInvolved(
                                aggregateId,
                                new ExecutedByHashed(select.getString("executed_by_hashed")),
                                new ExecutedByEncoded(select.getString("executed_by_encoded"))));
                    }
                }
                return new Page<>(content, pagination, content.size());
            } else {
                countPreparedStatement.setString(1, aggregateId.id());
                countPreparedStatement.setString(2, aggregateId.getClass().getSimpleName());
                selectPreparedStatement.setString(1, aggregateId.id());
                countPreparedStatement.setString(2, aggregateId.getClass().getSimpleName());
                selectPreparedStatement.setLong(3, pagination.size());
                selectPreparedStatement.setLong(2, pagination.offset());
                try (final ResultSet count = countPreparedStatement.executeQuery();
                     final ResultSet select = selectPreparedStatement.executeQuery()) {
                    count.next();
                    final long totalElements = count.getLong("count");
                    final List<EncodedInvolved> content = new ArrayList<>(pagination.size());
                    while (select.next()) {
                        content.add(new EncodedInvolved(
                                aggregateId,
                                new ExecutedByHashed(select.getString("executed_by_hashed")),
                                new ExecutedByEncoded(select.getString("executed_by_encoded"))));
                    }
                    return new Page<>(content, pagination, totalElements);
                }
            }
        } catch (final SQLException exception) {
            throw new TraceRepositoryException(exception);
        }
    }

    @Override
    public Page<EncodedInvolved> findBy(final ExecutedByHashed executedByHashed, final Pagination pagination) throws TraceRepositoryException {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(pagination);
        try (final Connection connection = dataSource.getConnection();
             // language=sql
             final PreparedStatement countPreparedStatement = connection.prepareStatement("""
                     SELECT COUNT(*) AS count FROM pulse.traceability_aggregate WHERE executed_by_hashed = ?
                     """);
             // language=sql
             final PreparedStatement selectPreparedStatement = connection.prepareStatement("""
                     SELECT aggregate_root_id, executed_by_encoded FROM pulse.traceability_aggregate  WHERE executed_by_hashed = ? LIMIT ? OFFSET ?
                     """)) {
            if (pagination.loadAll()) {
                selectPreparedStatement.setString(1, executedByHashed.hashed());
                final List<EncodedInvolved> content = new ArrayList<>();
                try (final ResultSet select = selectPreparedStatement.executeQuery()) {
                    while (select.next()) {
                        content.add(new EncodedInvolved(
                                new AnyAggregateId(select.getString("aggregate_root_type"),
                                        select.getString("aggregate_root_id")),
                                executedByHashed,
                                new ExecutedByEncoded(select.getString("executed_by_encoded"))));
                    }
                }
                return new Page<>(content, pagination, content.size());
            } else {
                countPreparedStatement.setString(1, executedByHashed.hashed());
                selectPreparedStatement.setString(1, executedByHashed.hashed());
                selectPreparedStatement.setLong(2, pagination.size());
                selectPreparedStatement.setLong(3, pagination.offset());
                try (final ResultSet count = countPreparedStatement.executeQuery();
                     final ResultSet select = selectPreparedStatement.executeQuery()) {
                    count.next();
                    final long totalElements = count.getLong("count");
                    final List<EncodedInvolved> content = new ArrayList<>(pagination.size());
                    while (select.next()) {
                        content.add(new EncodedInvolved(
                                new AnyAggregateId(select.getString("aggregate_root_type"),
                                        select.getString("aggregate_root_id")),
                                executedByHashed,
                                new ExecutedByEncoded(select.getString("executed_by_encoded"))));
                    }
                    return new Page<>(content, pagination, totalElements);
                }
            }
        } catch (final SQLException exception) {
            throw new TraceRepositoryException(exception);
        }
    }
}
