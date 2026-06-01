package com.damdamdeo.pulse.extension.encryption.storage.runtime.vault;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
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
@Transactional
public class JdbcPostgresPassphraseRepository implements PassphraseRepository {

    private final DataSource dataSource;
    private final Hasher hasher;

    public JdbcPostgresPassphraseRepository(final DataSource dataSource,
                                            final Hasher hasher) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.hasher = Objects.requireNonNull(hasher);
    }

    @Override
    public Optional<Passphrase> retrieve(final OwnedBy ownedBy) throws UnableToRetrievePassphraseException {
        Objects.requireNonNull(ownedBy);
        final String ownerHash = hash(ownedBy);
        final String sql =
                // language=sql
                """
                        SELECT passphrase
                        FROM passphrase
                        WHERE owned_by_hashed = ?
                        """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ownerHash);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Passphrase(rs.getString("passphrase").toCharArray()));
            }
        } catch (final SQLException sqlException) {
            throw new UnableToRetrievePassphraseException(sqlException);
        }
    }

    @Override
    public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException,
            UnableToStorePassphraseException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(passphrase);
        final String ownerHash = hash(ownedBy);
        final String sql =
                // language=sql
                """
                        INSERT INTO passphrase(owned_by_hashed, passphrase)
                        VALUES (?, ?)
                        """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ownerHash);
            stmt.setString(2, new String(passphrase.passphrase()));
            stmt.executeUpdate();
            return new Passphrase(passphrase.passphrase().clone());
        } catch (final SQLException sqlException) {
            if (isUniqueViolation(sqlException)) {
                throw new PassphraseAlreadyExistsException(ownedBy);
            } else {
                throw new UnableToStorePassphraseException(sqlException);
            }
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

    private String hash(final OwnedBy ownedBy) {
        return hasher.hash(ownedBy).value();
    }
}
