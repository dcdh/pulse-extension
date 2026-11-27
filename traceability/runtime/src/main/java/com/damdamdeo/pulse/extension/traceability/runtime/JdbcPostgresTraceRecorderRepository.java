package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.ApplicationNamingProvider;
import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.*;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class JdbcPostgresTraceRecorderRepository implements TraceRecorderRepository {

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
    public static final String SOURCE_COMMAND_TRACEABILITY_IDENTIFIABLE_SQL = """
            INSERT INTO %s.traceability_identifiable (
                identifiable,
                link_to_aggregate_root_id,
                executed_by_encoded_id,
                command_nb_of_times
            )
            VALUES (?, ?, ?, 1)
            ON CONFLICT (identifiable, executed_by_encoded_id)
            DO UPDATE
            SET command_nb_of_times = %1$s.traceability_identifiable.command_nb_of_times + 1;
            """;

    // language=sql
    public static final String SOURCE_COMMAND_UNAUTHORIZED_TRACEABILITY_IDENTIFIABLE_SQL = """
            INSERT INTO %s.traceability_identifiable (
                identifiable,
                link_to_aggregate_root_id,
                executed_by_encoded_id,
                command_unauthorized_nb_of_times
            )
            VALUES (?, ?, ?, 1)
            ON CONFLICT (identifiable, executed_by_encoded_id)
            DO UPDATE
            SET command_unauthorized_nb_of_times = %1$s.traceability_identifiable.command_unauthorized_nb_of_times + 1;
            """;

    // language=sql
    public static final String SOURCE_COMMAND_BUSINESS_FAILED_TRACEABILITY_IDENTIFIABLE_SQL = """
            INSERT INTO %s.traceability_identifiable (
                identifiable,
                link_to_aggregate_root_id,
                executed_by_encoded_id,
                command_business_failed_nb_of_times
            )
            VALUES (?, ?, ?, 1)
            ON CONFLICT (identifiable, executed_by_encoded_id)
            DO UPDATE
            SET command_business_failed_nb_of_times = %1$s.traceability_identifiable.command_business_failed_nb_of_times + 1;
            """;

    // language=sql
    public static final String SOURCE_QUERY_TRACEABILITY_IDENTIFIABLE_SQL = """
            INSERT INTO %s.traceability_identifiable (
                identifiable,
                link_to_aggregate_root_id,
                executed_by_encoded_id,
                query_nb_of_times
            )
            VALUES (?, ?, ?, 1)
            ON CONFLICT (identifiable, executed_by_encoded_id)
            DO UPDATE
            SET query_nb_of_times = %1$s.traceability_identifiable.query_nb_of_times + 1;
            """;

    // language=sql
    public static final String SOURCE_QUERY_UNAUTHORIZED_TRACEABILITY_IDENTIFIABLE_SQL = """
            INSERT INTO %s.traceability_identifiable (
                identifiable,
                link_to_aggregate_root_id,
                executed_by_encoded_id,
                query_unauthorized_nb_of_times
            )
            VALUES (?, ?, ?, 1)
            ON CONFLICT (identifiable, executed_by_encoded_id)
            DO UPDATE
            SET query_unauthorized_nb_of_times = %1$s.traceability_identifiable.query_unauthorized_nb_of_times + 1;
            """;

    // language=sql
    public static final String SOURCE_TRACEABILITY_IDENTIFIABLE_SQL = """
            INSERT INTO %s.traceability_identifiable (
                identifiable,
                link_to_aggregate_root_id,
                executed_by_encoded_id,
                traceability_nb_of_times
            )
            VALUES (?, ?, ?, 1)
            ON CONFLICT (identifiable, executed_by_encoded_id)
            DO UPDATE
            SET traceability_nb_of_times = %1$s.traceability_identifiable.traceability_nb_of_times + 1;
            """;

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
    public static final String INSERT_TRACEABILITY_DETAILS_TRACEABILITY_IDENTIFIABLE_SQL = """
            INSERT INTO %s.traceability_details_traceability_identifiable (
                traceability_details_id,
                traceability_identifiable_identifiable,
                traceability_identifiable_executed_by_encoded_id
            )
            VALUES (?, ?, ?)
            ON CONFLICT (traceability_details_id, traceability_identifiable_identifiable, traceability_identifiable_executed_by_encoded_id)
            DO NOTHING
            """;

    // language=sql
    public static final String INSERT_TRACEABILITY_DETAILS_AGGREGATE_EVENT_SQL = """
            INSERT INTO %s.traceability_aggregate_events (
                traceability_trace_id,
                identifiable,
                link_to_aggregate_root_id,
                event_type,
                aggregate_version
            )
            VALUES (?, ?, ?, ?, ?)
            """;

    private final TraceabilityConfiguration traceabilityConfiguration;
    private final DataSource dataSource;
    private final SchemaName schemaName;

    public JdbcPostgresTraceRecorderRepository(final TraceabilityConfiguration traceabilityConfiguration,
                                               final DataSource dataSource,
                                               final ApplicationNamingProvider applicationNamingProvider) {
        this.traceabilityConfiguration = Objects.requireNonNull(traceabilityConfiguration);
        this.dataSource = Objects.requireNonNull(dataSource);
        this.schemaName = SchemaName.from(applicationNamingProvider.provide());
    }

    @Override
    public void store(final TraceRecorder traceRecorder) throws TraceRepositoryException {
        Objects.requireNonNull(traceRecorder);
        store(traceRecorder, TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY);
    }

    @Override
    public void store(final TraceRecorder traceRecorder, final TraceabilityDetails traceabilityDetails) throws TraceRepositoryException {
        Objects.requireNonNull(traceRecorder);
        Objects.requireNonNull(traceabilityDetails);
        final Map<ExecutedByHashed, Long> idByExecutedByHashed = new HashMap<>();
        final String traceabilityAggregateSQL = switch (traceRecorder.source()) {
            case COMMAND -> switch (traceRecorder.executionStatus()) {
                case SUCCESS -> SOURCE_COMMAND_TRACEABILITY_IDENTIFIABLE_SQL;
                case FAILED_UNAUTHORIZED -> SOURCE_COMMAND_UNAUTHORIZED_TRACEABILITY_IDENTIFIABLE_SQL;
                case FAILED_BUSINESS -> SOURCE_COMMAND_BUSINESS_FAILED_TRACEABILITY_IDENTIFIABLE_SQL;
            };
            case QUERY -> switch (traceRecorder.executionStatus()) {
                case SUCCESS -> SOURCE_QUERY_TRACEABILITY_IDENTIFIABLE_SQL;
                case FAILED_UNAUTHORIZED -> SOURCE_QUERY_UNAUTHORIZED_TRACEABILITY_IDENTIFIABLE_SQL;
                case FAILED_BUSINESS -> throw new IllegalStateException("Should not be here");
            };
            case TRACEABILITY -> switch (traceRecorder.executionStatus()) {
                case SUCCESS -> SOURCE_TRACEABILITY_IDENTIFIABLE_SQL;
                case FAILED_UNAUTHORIZED, FAILED_BUSINESS -> throw new IllegalStateException("Should not be here");
            };
        };
        try (final Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (final PreparedStatement insertExecutedByEncodedPreparedStatement = connection.prepareStatement(
                    INSERT_EXECUTED_BY_ENCODED_SQL.formatted(schemaName.name()));
                 final PreparedStatement insertTraceabilityAggregatePreparedStatement = connection.prepareStatement(
                         traceabilityAggregateSQL.formatted(schemaName.name()))) {
                for (final EncodedTraceIdentifiable encodedTraceIdentifiable : traceRecorder.encodedTraceIdentifiables()) {
                    insertExecutedByEncodedPreparedStatement.setString(1, encodedTraceIdentifiable.executedByHashed().hashed());
                    insertExecutedByEncodedPreparedStatement.setString(2, encodedTraceIdentifiable.executedByEncoded().encoded());
                    insertExecutedByEncodedPreparedStatement.setString(3, encodedTraceIdentifiable.executedByHashed().hashed());
                    final long executedByEncodedId;
                    try (final ResultSet resultSet = insertExecutedByEncodedPreparedStatement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Unable to retrieve executed_by_encoded id");
                        }
                        executedByEncodedId = resultSet.getLong(1);
                    }
                    idByExecutedByHashed.put(encodedTraceIdentifiable.executedByHashed(), executedByEncodedId);
                    insertTraceabilityAggregatePreparedStatement.setString(1, encodedTraceIdentifiable.identifiable().id());
                    insertTraceabilityAggregatePreparedStatement.setString(2, encodedTraceIdentifiable.linkTo().id());
                    insertTraceabilityAggregatePreparedStatement.setLong(3, executedByEncodedId);
                    insertTraceabilityAggregatePreparedStatement.executeUpdate();
                }
            }
            if (TracingMode.INVOLVED_WITH_FULL_DETAILS.equals(traceabilityConfiguration.tracingMode())
                    && traceabilityDetails.equals(TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY)) {
                try (final PreparedStatement traceabilityDetailsPreparedStatement = connection.prepareStatement(
                        INSERT_TRACEABILITY_DETAILS_SQL.formatted(schemaName.name()));
                     final PreparedStatement insertTraceabilityDetailsTraceabilityIdentifiablePreparedStatement = connection.prepareStatement(
                             INSERT_TRACEABILITY_DETAILS_TRACEABILITY_IDENTIFIABLE_SQL.formatted(schemaName.name()));
                     final PreparedStatement insertTraceabilityDetailsAggregateEventPreparedStatement = connection.prepareStatement(
                             INSERT_TRACEABILITY_DETAILS_AGGREGATE_EVENT_SQL.formatted(schemaName.name()))) {
                    traceabilityDetailsPreparedStatement.setLong(1, traceRecorder.traceId().id());
                    traceabilityDetailsPreparedStatement.setLong(2, traceRecorder.correlationId().id());
                    traceabilityDetailsPreparedStatement.setTimestamp(3, Timestamp.from(traceRecorder.executedAt().at()));
                    traceabilityDetailsPreparedStatement.setInt(4, traceRecorder.source().ordinal());
                    traceabilityDetailsPreparedStatement.setInt(5, traceRecorder.executionStatus().ordinal());
                    traceabilityDetailsPreparedStatement.setString(6, traceRecorder.from().from());
                    traceabilityDetailsPreparedStatement.executeUpdate();
                    for (final EncodedTraceIdentifiable encodedTraceIdentifiable : traceRecorder.encodedTraceIdentifiables()) {
                        final long executedByEncodedId = Objects.requireNonNull(idByExecutedByHashed.get(
                                encodedTraceIdentifiable.executedByHashed()));
                        if (encodedTraceIdentifiable.hasEvent()) {
                            insertTraceabilityDetailsAggregateEventPreparedStatement.setLong(1, traceRecorder.traceId().id());
                            insertTraceabilityDetailsAggregateEventPreparedStatement.setString(2, encodedTraceIdentifiable.identifiable().id());
                            insertTraceabilityDetailsAggregateEventPreparedStatement.setString(3, encodedTraceIdentifiable.linkTo().id());
                            insertTraceabilityDetailsAggregateEventPreparedStatement.setString(4, encodedTraceIdentifiable.eventType().type());
                            insertTraceabilityDetailsAggregateEventPreparedStatement.setInt(5, encodedTraceIdentifiable.aggregateVersion().version());
                            insertTraceabilityDetailsAggregateEventPreparedStatement.executeUpdate();
                        }
                        insertTraceabilityDetailsTraceabilityIdentifiablePreparedStatement.setLong(1, traceRecorder.traceId().id());
                        insertTraceabilityDetailsTraceabilityIdentifiablePreparedStatement.setString(2, encodedTraceIdentifiable.identifiable().id());
                        insertTraceabilityDetailsTraceabilityIdentifiablePreparedStatement.setLong(3, executedByEncodedId);
                        insertTraceabilityDetailsTraceabilityIdentifiablePreparedStatement.executeUpdate();
                    }
                    connection.commit();
                } catch (final SQLException exception) {
                    try {
                        connection.rollback();
                    } catch (final SQLException rollbackFailure) {
                        exception.addSuppressed(rollbackFailure);
                    }
                    throw exception;
                }
            }
        } catch (final SQLException exception) {
            throw new TraceRepositoryException(exception);
        }
    }
}
