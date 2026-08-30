package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.ApplicationNamingProvider;
import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.ExecutedByEncodedRepository;
import com.damdamdeo.pulse.extension.core.traceability.ExecutedByEncodedRepositoryException;
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
public class JdbcPostgresExecutedByEncodedRepository implements ExecutedByEncodedRepository {

    // language=sql
    public static final String INSERT_EXECUTED_BY_ENCODED_SQL = """
            INSERT INTO %s.executed_by_encoded(executed_by_hashed, executed_by_encoded)
            VALUES (?, ?) ON CONFLICT (executed_by_hashed) DO NOTHING;
            """;

    // language=sql
    public static final String FIND_BY_EXECUTED_BY_HASHED_SQL = """
            SELECT executed_by_encoded FROM %s.executed_by_encoded WHERE executed_by_hashed = ?
            """;

    private final DataSource dataSource;
    private final SchemaName schemaName;

    public JdbcPostgresExecutedByEncodedRepository(final DataSource dataSource,
                                                   final ApplicationNamingProvider applicationNamingProvider) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.schemaName = SchemaName.from(applicationNamingProvider.provide());
    }

    @Override
    public ExecutedByEncoded findBy(final ExecutedByHashed executedByHashed) throws ExecutedByEncodedRepositoryException {
        Objects.requireNonNull(executedByHashed);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(
                     FIND_BY_EXECUTED_BY_HASHED_SQL.formatted(schemaName.name()))) {
            preparedStatement.setString(1, executedByHashed.hashed());
            final ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new ExecutedByEncoded(resultSet.getString("executed_by_encoded"));
            } else {
                return null;
            }
        } catch (final SQLException exception) {
            throw new ExecutedByEncodedRepositoryException(exception);
        }
    }

    @Override
    public void store(final ExecutedByHashed executedByHashed, final ExecutedByEncoded executedByEncoded)
            throws ExecutedByEncodedRepositoryException {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(executedByEncoded);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(
                     INSERT_EXECUTED_BY_ENCODED_SQL.formatted(schemaName.name()))) {
            preparedStatement.setString(1, executedByHashed.hashed());
            preparedStatement.setString(2, executedByEncoded.encoded());
            preparedStatement.executeUpdate();
        } catch (final SQLException exception) {
            throw new ExecutedByEncodedRepositoryException(exception);
        }
    }
}
