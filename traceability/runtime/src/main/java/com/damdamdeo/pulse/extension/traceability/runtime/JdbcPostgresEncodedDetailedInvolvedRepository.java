package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ApplicationNamingProvider;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.*;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class JdbcPostgresEncodedDetailedInvolvedRepository implements EncodedDetailedInvolvedRepository {

    private final DataSource dataSource;
    private final SchemaName schemaName;

    public JdbcPostgresEncodedDetailedInvolvedRepository(final DataSource dataSource,
                                                         final ApplicationNamingProvider applicationNamingProvider) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.schemaName = SchemaName.from(applicationNamingProvider.provide());
    }

    @Override
    public Page<EncodedDetailedInvolved> findBy(final AggregateId aggregateId, final IncludeUncompounded includeUncompounded,
                                                final Pagination pagination) throws TraceRepositoryException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(includeUncompounded);
        Objects.requireNonNull(pagination);
        try (final Connection connection = dataSource.getConnection();
             // language=sql
             final PreparedStatement countPreparedStatement = connection.prepareStatement(
                     """
                             SELECT COUNT(ta.*) AS count
                             FROM %1$s.traceability_aggregate ta
                             JOIN %1$s.traceability_details_traceability_aggregate tdta
                               ON tdta.traceability_aggregate_id = ta.id
                             WHERE ta.aggregate_root_id LIKE ?
                             """.formatted(schemaName.name()));
             // language=sql
             final PreparedStatement selectPreparedStatement = connection.prepareStatement("""
                     SELECT
                       ta.aggregate_root_id as aggregate_root_id,
                       ebe.executed_by_hashed AS executed_by_hashed,
                       ebe.executed_by_encoded AS executed_by_encoded,
                       td.trace_id AS trace_id,
                       td.from_value AS from_value,
                       td.executed_at AS executed_at
                     FROM %1$s.traceability_aggregate ta
                     JOIN %1$s.traceability_details_traceability_aggregate tdta
                       ON tdta.traceability_aggregate_id = ta.id
                     JOIN %1$s.traceability_details td
                       ON td.trace_id = tdta.traceability_details_id
                     JOIN %1$s.executed_by_encoded ebe
                       ON ebe.id = ta.executed_by_encoded_id
                     WHERE ta.aggregate_root_id LIKE ?
                     ORDER BY td.trace_id
                     LIMIT ? OFFSET ?
                     """.formatted(schemaName.name()))) {
            if (pagination.loadAll()) {
                if (!includeUncompounded.included()) {
                    selectPreparedStatement.setString(1, aggregateId.id());
                } else {
                    selectPreparedStatement.setString(1, aggregateId.id() + "%");
                }
                selectPreparedStatement.setNull(2, Types.INTEGER);
                selectPreparedStatement.setLong(3, 0);
                final List<EncodedDetailedInvolved> content = new ArrayList<>();
                try (final ResultSet select = selectPreparedStatement.executeQuery()) {
                    while (select.next()) {
                        content.add(new EncodedDetailedInvolved(
                                new TraceId(select.getLong("trace_id")),
                                new AnyAggregateId(select.getString("aggregate_root_id")),
                                new EncodedActor(
                                        new ExecutedByHashed(select.getString("executed_by_hashed")),
                                        new ExecutedByEncoded(select.getString("executed_by_encoded"))
                                ),
                                new From(select.getString("from_value")),
                                new ExecutedAt(select.getTimestamp("executed_at").toInstant())
                        ));
                    }
                }
                return new Page<>(content, pagination, content.size());
            } else {
                if (!includeUncompounded.included()) {
                    countPreparedStatement.setString(1, aggregateId.id());
                    selectPreparedStatement.setString(1, aggregateId.id());
                } else {
                    countPreparedStatement.setString(1, aggregateId.id() + "%");
                    selectPreparedStatement.setString(1, aggregateId.id() + "%");
                }
                selectPreparedStatement.setLong(2, pagination.size());
                selectPreparedStatement.setLong(3, pagination.offset());
                try (final ResultSet count = countPreparedStatement.executeQuery();
                     final ResultSet select = selectPreparedStatement.executeQuery()) {
                    count.next();
                    final long totalElements = count.getLong("count");
                    final List<EncodedDetailedInvolved> content = new ArrayList<>(pagination.size());
                    while (select.next()) {
                        content.add(new EncodedDetailedInvolved(
                                new TraceId(select.getLong("trace_id")),
                                new AnyAggregateId(select.getString("aggregate_root_id")),
                                new EncodedActor(
                                        new ExecutedByHashed(select.getString("executed_by_hashed")),
                                        new ExecutedByEncoded(select.getString("executed_by_encoded"))
                                ),
                                new From(select.getString("from_value")),
                                new ExecutedAt(select.getTimestamp("executed_at").toInstant())
                        ));
                    }
                    return new Page<>(content, pagination, totalElements);
                }
            }
        } catch (final SQLException exception) {
            throw new TraceRepositoryException(exception);
        }
    }

    @Override
    public Page<EncodedDetailedInvolved> findBy(final ExecutedByHashed executedByHashed, final Pagination pagination) throws TraceRepositoryException {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(pagination);
        try (final Connection connection = dataSource.getConnection();
             // language=sql
             final PreparedStatement countPreparedStatement = connection.prepareStatement(
                     """                     
                             SELECT COUNT(ta.*) AS count
                             FROM %1$s.traceability_aggregate ta
                             JOIN %1$s.traceability_details_traceability_aggregate tdta
                               ON tdta.traceability_aggregate_id = ta.id
                             JOIN %1$s.executed_by_encoded ebe
                               ON ebe.id = ta.executed_by_encoded_id
                             WHERE ebe.executed_by_hashed = ?
                             """.formatted(schemaName.name()));
             // language=sql
             final PreparedStatement selectPreparedStatement = connection.prepareStatement("""
                     SELECT
                         ta.aggregate_root_id AS aggregate_root_id,
                         ebe.executed_by_hashed AS executed_by_hashed,
                         ebe.executed_by_encoded AS executed_by_encoded,
                         td.trace_id AS trace_id,
                         td.from_value AS from_value,
                         td.executed_at AS executed_at
                     FROM %1$s.traceability_aggregate ta
                     JOIN %1$s.traceability_details_traceability_aggregate tdta
                       ON tdta.traceability_aggregate_id = ta.id
                     JOIN %1$s.traceability_details td
                       ON td.trace_id = tdta.traceability_details_id
                     JOIN %1$s.executed_by_encoded ebe
                       ON ebe.id = ta.executed_by_encoded_id
                     WHERE ebe.executed_by_hashed = ?
                     ORDER BY td.trace_id
                     LIMIT ? OFFSET ?
                     """.formatted(schemaName.name()))) {
            if (pagination.loadAll()) {
                selectPreparedStatement.setString(1, executedByHashed.hashed());
                selectPreparedStatement.setNull(2, Types.INTEGER);
                selectPreparedStatement.setLong(3, 0);
                final List<EncodedDetailedInvolved> content = new ArrayList<>();
                try (final ResultSet select = selectPreparedStatement.executeQuery()) {
                    while (select.next()) {
                        content.add(new EncodedDetailedInvolved(
                                new TraceId(select.getLong("trace_id")),
                                new AnyAggregateId(select.getString("aggregate_root_id")),
                                new EncodedActor(
                                        new ExecutedByHashed(select.getString("executed_by_hashed")),
                                        new ExecutedByEncoded(select.getString("executed_by_encoded"))
                                ),
                                new From(select.getString("from_value")),
                                new ExecutedAt(select.getTimestamp("executed_at").toInstant())
                        ));
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
                    final List<EncodedDetailedInvolved> content = new ArrayList<>(pagination.size());
                    while (select.next()) {
                        content.add(new EncodedDetailedInvolved(
                                new TraceId(select.getLong("trace_id")),
                                new AnyAggregateId(select.getString("aggregate_root_id")),
                                new EncodedActor(
                                        new ExecutedByHashed(select.getString("executed_by_hashed")),
                                        new ExecutedByEncoded(select.getString("executed_by_encoded"))
                                ),
                                new From(select.getString("from_value")),
                                new ExecutedAt(select.getTimestamp("executed_at").toInstant())
                        ));
                    }
                    return new Page<>(content, pagination, totalElements);
                }
            }
        } catch (final SQLException exception) {
            throw new TraceRepositoryException(exception);
        }
    }
}
