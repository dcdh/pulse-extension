package com.damdamdeo.pulse.extension.common.runtime.datasource;

import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifier;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepositoryException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.DuplicateConnectionIdentifierException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
@Unremovable
public class JdbcPostgresConnectionIdentifierRepository implements ConnectionIdentifierRepository {

    private static final String INSERT_SQL =
            // language=sql
            """
                    INSERT INTO pulse.connection_identifier (
                        connection_identifier_hash,
                        identifiable_id
                    )
                    VALUES (?, ?)
                    ON CONFLICT (connection_identifier_hash) DO NOTHING
                    """;

    private static final String SELECT_BY_CONNECTION_IDENTIFIER_SQL =
            // language=sql
            """
                    SELECT identifiable_id
                    FROM pulse.connection_identifier
                    WHERE connection_identifier_hash = ?
                    """;

    record AnyIdentifiable(String id) implements Identifiable {

        AnyIdentifiable {
            Objects.requireNonNull(id);
        }
    }

    @Inject
    Provider<DataSource> dataSource;

    @Override
    @Transactional(value = Transactional.TxType.MANDATORY)
    public ConnectionIdentifier store(final ConnectionIdentifier connectionIdentifier, final Identifiable identifiable)
            throws DuplicateConnectionIdentifierException, ConnectionIdentifierRepositoryException {
        Objects.requireNonNull(connectionIdentifier);
        Objects.requireNonNull(identifiable);
        try (final Connection connection = dataSource.get().getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL)) {
            preparedStatement.setString(1, connectionIdentifier.id());
            preparedStatement.setString(2, identifiable.id());
            final int updated = preparedStatement.executeUpdate();
            if (updated == 0) {
                throw new DuplicateConnectionIdentifierException(
                        "Connection identifier hash already exists: %s.".formatted(connectionIdentifier.id()));
            }
        } catch (final SQLException exception) {
            throw new ConnectionIdentifierRepositoryException("Unable to store connection identifier.", exception);
        }
        return connectionIdentifier;
    }

    @Override
    public Optional<Identifiable> find(final ConnectionIdentifier connectionIdentifier) throws ConnectionIdentifierRepositoryException {
        Objects.requireNonNull(connectionIdentifier);
        try (final Connection connection = dataSource.get().getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BY_CONNECTION_IDENTIFIER_SQL)) {
            preparedStatement.setString(1, connectionIdentifier.id());
            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AnyIdentifiable(resultSet.getString("identifiable_id")));
            }
        } catch (final SQLException exception) {
            throw new ConnectionIdentifierRepositoryException("Unable to find identifiable by hash.", exception);
        }
    }
}

