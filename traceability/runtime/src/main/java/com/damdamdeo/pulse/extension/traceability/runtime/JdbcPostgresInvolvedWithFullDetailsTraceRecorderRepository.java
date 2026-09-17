package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.ApplicationNamingProvider;
import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import com.damdamdeo.pulse.extension.core.traceability.EncodedTraceAggregateId;
import com.damdamdeo.pulse.extension.core.traceability.TraceRecorder;
import com.damdamdeo.pulse.extension.core.traceability.TraceRecorderRepository;
import com.damdamdeo.pulse.extension.core.traceability.TraceRepositoryException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository implements TraceRecorderRepository {

    // executed_by_hashed is determinist
    // executed_by_encoded is non-deterministic even on the same username
    // language=sql
    public static final String TRACEABILITY_DETAILS_SQL = """
            INSERT INTO %s.traceability_details (
                trace_id,
                executed_at,
                from_value
            )
            VALUES (?, ?, ?);
            """;

    // language=sql
    public static final String EXECUTED_BY_ENCODED_SQL = """
             WITH inserted AS (
                INSERT INTO %1$s.executed_by_encoded (
                    executed_by_hashed,
                    executed_by_encoded
                )
                VALUES (?, ?)
                ON CONFLICT (executed_by_hashed)
                DO NOTHING
                RETURNING id
            )
            SELECT id
            FROM inserted
            
            UNION ALL
            
            SELECT id
            FROM %1$s.executed_by_encoded
            WHERE executed_by_hashed = ?
            LIMIT 1;
            """;

    // language=sql
    public static final String TRACEABILITY_AGGREGATE_SQL = """
            INSERT INTO %s.traceability_aggregate (
                aggregate_root_id,
                executed_by_encoded_id,
                nb_of_times
            )
            VALUES (?, ?, 1)
            ON CONFLICT (aggregate_root_id, executed_by_encoded_id)
            DO UPDATE
            SET nb_of_times = %1$s.traceability_aggregate.nb_of_times + 1
            RETURNING id;
            """;

    // language=sql
    public static final String TRACEABILITY_DETAILS_TRACEABILITY_AGGREGATE_SQL = """
            INSERT INTO %s.traceability_details_traceability_aggregate (
                traceability_details_id,
                traceability_aggregate_id
            )
            VALUES (?, ?)
            ON CONFLICT (traceability_details_id, traceability_aggregate_id)
            DO NOTHING
            """;

    private final DataSource dataSource;
    private final SchemaName schemaName;

    public JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository(final DataSource dataSource,
                                                                      final ApplicationNamingProvider applicationNamingProvider) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.schemaName = SchemaName.from(applicationNamingProvider.provide());
    }

    @Override
    public void store(final TraceRecorder traceRecorder) throws TraceRepositoryException {
        Objects.requireNonNull(traceRecorder);
        try (final Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (final PreparedStatement traceabilityDetailsPreparedStatement = connection.prepareStatement(
                    TRACEABILITY_DETAILS_SQL.formatted(schemaName.name()));
                 final PreparedStatement executedByEncodedPreparedStatement = connection.prepareStatement(
                         EXECUTED_BY_ENCODED_SQL.formatted(schemaName.name()));
                 final PreparedStatement traceabilityAggregatePreparedStatement = connection.prepareStatement(
                         TRACEABILITY_AGGREGATE_SQL.formatted(schemaName.name()));
                 final PreparedStatement traceabilityDetailsTraceabilityAggregatePreparedStatement = connection.prepareStatement(
                         TRACEABILITY_DETAILS_TRACEABILITY_AGGREGATE_SQL.formatted(schemaName.name()))) {
                traceabilityDetailsPreparedStatement.setLong(1, traceRecorder.traceId().id());
                traceabilityDetailsPreparedStatement.setTimestamp(2, Timestamp.from(traceRecorder.executedAt().at()));
                traceabilityDetailsPreparedStatement.setString(3, traceRecorder.from().from());
                traceabilityDetailsPreparedStatement.executeUpdate();
                for (final EncodedTraceAggregateId encodedTraceAggregateId : traceRecorder.encodedTraceAggregateIds()) {
                    executedByEncodedPreparedStatement.setString(1, encodedTraceAggregateId.executedByHashed().hashed());
                    executedByEncodedPreparedStatement.setString(2, encodedTraceAggregateId.executedByEncoded().encoded());
                    executedByEncodedPreparedStatement.setString(3, encodedTraceAggregateId.executedByHashed().hashed());
                    final long executedByEncodedId;
                    try (final ResultSet resultSet = executedByEncodedPreparedStatement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Unable to retrieve executed_by_encoded id");
                        }
                        executedByEncodedId = resultSet.getLong(1);
                    }
                    traceabilityAggregatePreparedStatement.setString(1, encodedTraceAggregateId.aggregateId().id());
                    traceabilityAggregatePreparedStatement.setLong(2, executedByEncodedId);
                    final long traceabilityAggregateId;
                    try (final ResultSet resultSet = traceabilityAggregatePreparedStatement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Unable to retrieve traceability_aggregate id");
                        }
                        traceabilityAggregateId = resultSet.getLong(1);
                    }
                    traceabilityDetailsTraceabilityAggregatePreparedStatement.setLong(1, traceRecorder.traceId().id());
                    traceabilityDetailsTraceabilityAggregatePreparedStatement.setLong(2, traceabilityAggregateId);
                    traceabilityDetailsTraceabilityAggregatePreparedStatement.executeUpdate();
                }
                connection.commit();
            } catch (final SQLException exception) {
                connection.rollback();
                throw new TraceRepositoryException(exception);
            }
        } catch (final SQLException exception) {
            throw new TraceRepositoryException(exception);
        }
    }
}
