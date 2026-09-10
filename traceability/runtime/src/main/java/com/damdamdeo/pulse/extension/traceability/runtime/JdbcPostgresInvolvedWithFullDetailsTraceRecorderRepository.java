package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.traceability.EncodedTraceAggregateId;
import com.damdamdeo.pulse.extension.core.traceability.TraceRecorder;
import com.damdamdeo.pulse.extension.core.traceability.TraceRecorderRepository;
import com.damdamdeo.pulse.extension.core.traceability.TraceRepositoryException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository implements TraceRecorderRepository {

    // language=sql
    public static final String TRACEABILITY_DETAILS_SQL = """
            INSERT INTO pulse.traceability_details(trace_id, executed_at, executed_status, from) VALUES (?, ?, ?, ?);
            """;

    // language=sql
    public static final String TRACEABILITY_AGGREGATE_SQL = """
            INSERT INTO pulse.traceability_aggregate(trace_id, aggregate_root_id, aggregate_root_type, executed_by_hashed, executed_by_encoded) VALUES (?, ?, ?, ?, ?);
            """;

    private final DataSource dataSource;

    public JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public void store(final TraceRecorder traceRecorder) throws TraceRepositoryException {
        Objects.requireNonNull(traceRecorder);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement traceabilityDetailsPreparedStatement = connection.prepareStatement(TRACEABILITY_DETAILS_SQL);
             final PreparedStatement traceabilityAggregatePreparedStatement = connection.prepareStatement(TRACEABILITY_AGGREGATE_SQL)) {
            traceabilityDetailsPreparedStatement.setLong(1, traceRecorder.traceId().id());
            traceabilityDetailsPreparedStatement.setTimestamp(2, Timestamp.from(traceRecorder.executedAt().at()));
            traceabilityDetailsPreparedStatement.setInt(3, traceRecorder.executionStatus().ordinal());
            traceabilityDetailsPreparedStatement.setString(4, traceRecorder.from().from());

            for (final EncodedTraceAggregateId encodedTraceAggregateId : traceRecorder.encodedTraceAggregateIds()) {
                traceabilityAggregatePreparedStatement.setLong(1, traceRecorder.traceId().id());
                traceabilityAggregatePreparedStatement.setString(2, encodedTraceAggregateId.aggregateId().id());
                traceabilityAggregatePreparedStatement.setString(3, encodedTraceAggregateId.aggregateId().getClass().getSimpleName());
                traceabilityAggregatePreparedStatement.setString(4, encodedTraceAggregateId.executedByHashed().hashed());
                traceabilityAggregatePreparedStatement.setString(5, encodedTraceAggregateId.executedByEncoded().encoded());
                traceabilityAggregatePreparedStatement.addBatch();
            }
            traceabilityDetailsPreparedStatement.executeQuery();
            traceabilityAggregatePreparedStatement.executeBatch();
        } catch (final SQLException exception) {
            throw new TraceRepositoryException(exception);
        }
    }
}
