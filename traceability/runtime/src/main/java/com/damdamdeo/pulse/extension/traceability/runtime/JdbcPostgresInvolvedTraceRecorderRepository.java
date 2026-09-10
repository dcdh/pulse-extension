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
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class JdbcPostgresInvolvedTraceRecorderRepository implements TraceRecorderRepository {

    // language=sql
    public static final String INSERT_TRACEABILITY_AGGREGATE_SQL = """
            INSERT INTO pulse.traceability_aggregate(aggregate_root_id, aggregate_root_type, executed_by_hashed, executed_by_encoded)
            VALUES (?, ?, ?, ?) ON CONFLICT (aggregate_root_id, aggregate_root_type, executed_by_hashed) DO NOTHING;
            """;

    private final DataSource dataSource;

    public JdbcPostgresInvolvedTraceRecorderRepository(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public void store(final TraceRecorder traceRecorder) throws TraceRepositoryException {
        Objects.requireNonNull(traceRecorder);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_TRACEABILITY_AGGREGATE_SQL)) {
            for (final EncodedTraceAggregateId encodedTraceAggregateId : traceRecorder.encodedTraceAggregateIds()) {
                preparedStatement.setString(1, encodedTraceAggregateId.aggregateId().id());
                preparedStatement.setString(2, encodedTraceAggregateId.aggregateId().getClass().getSimpleName());
                preparedStatement.setString(3, encodedTraceAggregateId.executedByHashed().hashed());
                preparedStatement.setString(4, encodedTraceAggregateId.executedByEncoded().encoded());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (final SQLException exception) {
            throw new TraceRepositoryException(exception);
        }
    }
}
