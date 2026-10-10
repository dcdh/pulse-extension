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

import static com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresInvolvedTraceRecorderRepository.*;

@ApplicationScoped
@Unremovable
public class JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository implements TraceRecorderRepository {

    // executed_by_hashed is determinist
    // executed_by_encoded is non-deterministic even on the same username
    // language=sql
    public static final String INSERT_TRACEABILITY_DETAILS_SQL = """
            INSERT INTO %s.traceability_details (
                trace_id,
                correlation_id,
                executed_at,
                source_value,
                execution_status,
                from_value
            )
            VALUES (?, ?, ?, ?, ?, ?);
            """;

    // language=sql
    public static final String INSERT_TRACEABILITY_DETAILS_TRACEABILITY_AGGREGATE_SQL = """
            INSERT INTO %s.traceability_details_traceability_aggregate (
                traceability_details_id,
                traceability_aggregate_id
            )
            VALUES (?, ?)
            ON CONFLICT (traceability_details_id, traceability_aggregate_id)
            DO NOTHING
            """;

    // language=sql
    public static final String INSERT_TRACEABILITY_AGGREGATE_EVENT_SQL = """
            INSERT INTO %s.traceability_aggregate_events (
                traceability_trace_id,
                event_type,
                aggregate_version
            )
            VALUES (?, ?, ?)
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
        final String traceabilityAggregateSQL = switch (traceRecorder.source()) {
            case COMMAND -> switch (traceRecorder.executionStatus()) {
                case SUCCESS -> SOURCE_COMMAND_TRACEABILITY_AGGREGATE_SQL;
                case FAILED_UNAUTHORIZED -> SOURCE_COMMAND_UNAUTHORIZED_TRACEABILITY_AGGREGATE_SQL;
                case FAILED_BUSINESS -> SOURCE_COMMAND_BUSINESS_FAILED_TRACEABILITY_AGGREGATE_SQL;
            };
            case QUERY -> switch (traceRecorder.executionStatus()) {
                case SUCCESS -> SOURCE_QUERY_TRACEABILITY_AGGREGATE_SQL;
                case FAILED_UNAUTHORIZED -> SOURCE_QUERY_UNAUTHORIZED_TRACEABILITY_AGGREGATE_SQL;
                case FAILED_BUSINESS -> throw new IllegalStateException("Should not be here");
            };
            case TRACEABILITY -> switch (traceRecorder.executionStatus()) {
                case SUCCESS -> SOURCE_TRACEABILITY_AGGREGATE_SQL;
                case FAILED_UNAUTHORIZED, FAILED_BUSINESS -> throw new IllegalStateException("Should not be here");
            };
        };
        try (final Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (final PreparedStatement traceabilityDetailsPreparedStatement = connection.prepareStatement(
                    INSERT_TRACEABILITY_DETAILS_SQL.formatted(schemaName.name()));
                 final PreparedStatement insertExecutedByEncodedPreparedStatement = connection.prepareStatement(
                         JdbcPostgresInvolvedTraceRecorderRepository.INSERT_EXECUTED_BY_ENCODED_SQL.formatted(schemaName.name()));
                 final PreparedStatement insertTraceabilityAggregatePreparedStatement = connection.prepareStatement(
                         traceabilityAggregateSQL.formatted(schemaName.name()));
                 final PreparedStatement insertTraceabilityDetailsTraceabilityAggregatePreparedStatement = connection.prepareStatement(
                         INSERT_TRACEABILITY_DETAILS_TRACEABILITY_AGGREGATE_SQL.formatted(schemaName.name()));
                 final PreparedStatement insertTraceabilityAggregateEventPreparedStatement = connection.prepareStatement(
                         INSERT_TRACEABILITY_AGGREGATE_EVENT_SQL.formatted(schemaName.name()))) {
                traceabilityDetailsPreparedStatement.setLong(1, traceRecorder.traceId().id());
                traceabilityDetailsPreparedStatement.setLong(2, traceRecorder.correlationId().id());
                traceabilityDetailsPreparedStatement.setTimestamp(3, Timestamp.from(traceRecorder.executedAt().at()));
                traceabilityDetailsPreparedStatement.setInt(4, traceRecorder.source().ordinal());
                traceabilityDetailsPreparedStatement.setInt(5, traceRecorder.executionStatus().ordinal());
                traceabilityDetailsPreparedStatement.setString(6, traceRecorder.from().from());
                traceabilityDetailsPreparedStatement.executeUpdate();
                for (final EncodedTraceAggregateId encodedTraceAggregateId : traceRecorder.encodedTraceAggregateIds()) {
                    insertExecutedByEncodedPreparedStatement.setString(1, encodedTraceAggregateId.executedByHashed().hashed());
                    insertExecutedByEncodedPreparedStatement.setString(2, encodedTraceAggregateId.executedByEncoded().encoded());
                    insertExecutedByEncodedPreparedStatement.setString(3, encodedTraceAggregateId.executedByHashed().hashed());
                    final long executedByEncodedId;
                    try (final ResultSet resultSet = insertExecutedByEncodedPreparedStatement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Unable to retrieve executed_by_encoded id");
                        }
                        executedByEncodedId = resultSet.getLong(1);
                    }
                    insertTraceabilityAggregatePreparedStatement.setString(1, encodedTraceAggregateId.aggregateId().id());
                    insertTraceabilityAggregatePreparedStatement.setLong(2, executedByEncodedId);
                    final long traceabilityAggregateId;
                    try (final ResultSet resultSet = insertTraceabilityAggregatePreparedStatement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Unable to retrieve traceability_aggregate id");
                        }
                        traceabilityAggregateId = resultSet.getLong(1);
                    }
                    if (encodedTraceAggregateId.hasEvent()) {
                        insertTraceabilityAggregateEventPreparedStatement.setLong(1, traceRecorder.traceId().id());
                        insertTraceabilityAggregateEventPreparedStatement.setString(2, encodedTraceAggregateId.eventType().type());
                        insertTraceabilityAggregateEventPreparedStatement.setInt(3, encodedTraceAggregateId.aggregateVersion().version());
                        insertTraceabilityAggregateEventPreparedStatement.executeUpdate();
                    }
                    insertTraceabilityDetailsTraceabilityAggregatePreparedStatement.setLong(1, traceRecorder.traceId().id());
                    insertTraceabilityDetailsTraceabilityAggregatePreparedStatement.setLong(2, traceabilityAggregateId);
                    insertTraceabilityDetailsTraceabilityAggregatePreparedStatement.executeUpdate();
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
