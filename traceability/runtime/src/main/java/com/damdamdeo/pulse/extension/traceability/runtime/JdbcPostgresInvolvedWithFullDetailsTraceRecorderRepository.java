package com.damdamdeo.pulse.extension.traceability.runtime;

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
            INSERT INTO pulse.traceability_details (
                trace_id,
                executed_at,
                from_value
            )
            VALUES (?, ?, ?);
            """;

    // language=sql
    public static final String EXECUTED_BY_ENCODED_SQL = """
            INSERT INTO pulse.executed_by_encoded (
                executed_by_hashed,
                executed_by_encoded
            )
            VALUES (?, ?)
            ON CONFLICT (executed_by_hashed)
            DO NOTHING
            RETURNING id;
            """;

    // language=sql
    public static final String TRACEABILITY_AGGREGATE_SQL = """
            INSERT INTO pulse.traceability_aggregate (
                trace_id,
                aggregate_root_id,
                executed_by_encoded_id
            )
            VALUES (?, ?, ?)
            ON CONFLICT (aggregate_root_id, executed_by_encoded_id)
            DO NOTHING;
            """;

    private final DataSource dataSource;

    public JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public void store(final TraceRecorder traceRecorder) throws TraceRepositoryException {
        Objects.requireNonNull(traceRecorder);
        try (final Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (final PreparedStatement traceabilityDetailsPreparedStatement = connection.prepareStatement(TRACEABILITY_DETAILS_SQL);
                 final PreparedStatement executedByEncodedPreparedStatement = connection.prepareStatement(EXECUTED_BY_ENCODED_SQL);
                 final PreparedStatement traceabilityAggregatePreparedStatement = connection.prepareStatement(TRACEABILITY_AGGREGATE_SQL)) {
                traceabilityDetailsPreparedStatement.setLong(1, traceRecorder.traceId().id());
                traceabilityDetailsPreparedStatement.setTimestamp(2, Timestamp.from(traceRecorder.executedAt().at()));
                traceabilityDetailsPreparedStatement.setString(3, traceRecorder.from().from());
                traceabilityDetailsPreparedStatement.executeUpdate();
                for (final EncodedTraceAggregateId encodedTraceAggregateId : traceRecorder.encodedTraceAggregateIds()) {
                    executedByEncodedPreparedStatement.setString(1, encodedTraceAggregateId.executedByHashed().hashed());
                    executedByEncodedPreparedStatement.setString(2, encodedTraceAggregateId.executedByEncoded().encoded());
                    final long executedByEncodedId;
                    try (final ResultSet resultSet = executedByEncodedPreparedStatement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Unable to retrieve executed_by_encoded id");
                        }
                        executedByEncodedId = resultSet.getLong(1);
                    }
                    traceabilityAggregatePreparedStatement.setLong(1, traceRecorder.traceId().id());
                    traceabilityAggregatePreparedStatement.setString(2, encodedTraceAggregateId.aggregateId().id());
                    traceabilityAggregatePreparedStatement.setLong(3, executedByEncodedId);
                    traceabilityAggregatePreparedStatement.addBatch();
                }
                traceabilityAggregatePreparedStatement.executeBatch();
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
