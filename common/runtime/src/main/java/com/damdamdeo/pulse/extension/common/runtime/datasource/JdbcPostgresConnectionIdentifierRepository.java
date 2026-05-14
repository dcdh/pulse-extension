package com.damdamdeo.pulse.extension.common.runtime.datasource;

import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifier;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepositoryException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.DuplicateConnectionIdentifierException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
                    INSERT INTO connection_identifier (
                        connection_identifier_hash,
                        identifiable_id
                    )
                    VALUES (?, ?)
                    """;

    private static final String SELECT_SQL =
            // language=sql
            """
                    SELECT identifiable_id
                    FROM connection_identifier
                    WHERE connection_identifier_hash = ?
                    """;

    record AnyIdentifiable(String id) implements Identifiable {

        AnyIdentifiable {
            Objects.requireNonNull(id);
        }
    }

    @Inject
    DataSource dataSource;

    @Override
    public void store(final Hash<ConnectionIdentifier> connectionIdentifierHash, final Identifiable identifiable)
            throws DuplicateConnectionIdentifierException, ConnectionIdentifierRepositoryException {
        Objects.requireNonNull(connectionIdentifierHash);
        Objects.requireNonNull(identifiable);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL)) {
            preparedStatement.setString(1, connectionIdentifierHash.value());
            preparedStatement.setString(2, identifiable.id());
            preparedStatement.executeUpdate();
        } catch (final SQLException exception) {
            if (isUniqueViolation(exception)) {
                throw new DuplicateConnectionIdentifierException(
                        "Connection identifier hash already exists: %s.".formatted(connectionIdentifierHash.value()),
                        exception);
            }
            throw new ConnectionIdentifierRepositoryException("Unable to store connection identifier.",
                    exception);
        }
    }

    @Override
    public Optional<Identifiable> findByHash(final Hash<ConnectionIdentifier> connectionIdentifierHash) throws ConnectionIdentifierRepositoryException {
        Objects.requireNonNull(connectionIdentifierHash);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL)) {
            preparedStatement.setString(1, connectionIdentifierHash.value());
            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(
                        new AnyIdentifiable(
                                resultSet.getString("identifiable_id")));
            }
        } catch (final SQLException exception) {
            throw new ConnectionIdentifierRepositoryException("Unable to find identifiable by hash.", exception);
        }
    }

    private boolean isUniqueViolation(final SQLException exception) {
        SQLException current = exception;
        while (current != null) {
            /*
             * PostgreSQL unique violation SQL state
             */
            if ("23505".equals(current.getSQLState())) {
                return true;
            }
            current = current.getNextException();
        }
        return false;
    }
}

