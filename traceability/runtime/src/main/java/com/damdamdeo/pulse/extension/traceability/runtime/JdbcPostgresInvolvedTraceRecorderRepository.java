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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class JdbcPostgresInvolvedTraceRecorderRepository implements TraceRecorderRepository {

    // executed_by_hashed is determinist
    // executed_by_encoded is non-deterministic even on the same username

    // language=sql
    public static final String INSERT_EXECUTED_BY_ENCODED_SQL = """
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
    public static final String INSERT_TRACEABILITY_AGGREGATE_SQL = """
            INSERT INTO %s.traceability_aggregate (
                aggregate_root_id,
                executed_by_encoded_id
            )
            VALUES (?, ?)
            ON CONFLICT (aggregate_root_id, executed_by_encoded_id)
            DO NOTHING;
            """;

    private final DataSource dataSource;
    private final SchemaName schemaName;

    public JdbcPostgresInvolvedTraceRecorderRepository(final DataSource dataSource,
                                                       final ApplicationNamingProvider applicationNamingProvider) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.schemaName = SchemaName.from(applicationNamingProvider.provide());
    }

    @Override
    public void store(final TraceRecorder traceRecorder) throws TraceRepositoryException {
        Objects.requireNonNull(traceRecorder);
        try (final Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (final PreparedStatement executedByStatement = connection.prepareStatement(
                    INSERT_EXECUTED_BY_ENCODED_SQL.formatted(schemaName.name()));
                 final PreparedStatement aggregateStatement = connection.prepareStatement(
                         INSERT_TRACEABILITY_AGGREGATE_SQL.formatted(schemaName.name()))) {
                for (final EncodedTraceAggregateId encodedTraceAggregateId : traceRecorder.encodedTraceAggregateIds()) {
                    final long executedByEncodedId;
                    executedByStatement.setString(1, encodedTraceAggregateId.executedByHashed().hashed());
                    executedByStatement.setString(2, encodedTraceAggregateId.executedByEncoded().encoded());
                    executedByStatement.setString(3, encodedTraceAggregateId.executedByHashed().hashed());
                    try (final ResultSet resultSet = executedByStatement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Unable to retrieve executed_by_encoded id");
                        }
                        executedByEncodedId = resultSet.getLong(1);
                    }
                    aggregateStatement.setString(1, encodedTraceAggregateId.aggregateId().id());
                    aggregateStatement.setLong(2, executedByEncodedId);
                    aggregateStatement.addBatch();
                }
                aggregateStatement.executeBatch();
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
