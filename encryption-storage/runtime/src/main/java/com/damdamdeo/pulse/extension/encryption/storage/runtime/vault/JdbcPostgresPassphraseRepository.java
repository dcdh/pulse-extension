package com.damdamdeo.pulse.extension.encryption.storage.runtime.vault;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
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
@Transactional
@DefaultBean
public class JdbcPostgresPassphraseRepository implements PassphraseRepository {

    private final PassphraseConfiguration passphraseConfiguration;
    private final Provider<DataSource> dataSource;
    private final Hasher hasher;
    private final PassphraseObfuscator passphraseObfuscator;

    public JdbcPostgresPassphraseRepository(final PassphraseConfiguration passphraseConfiguration,
                                            final Provider<DataSource> dataSource,
                                            final Hasher hasher,
                                            final PassphraseObfuscator passphraseObfuscator) {
        this.passphraseConfiguration = Objects.requireNonNull(passphraseConfiguration);
        this.dataSource = Objects.requireNonNull(dataSource);
        this.hasher = Objects.requireNonNull(hasher);
        this.passphraseObfuscator = Objects.requireNonNull(passphraseObfuscator);
    }

    @Override
    public Optional<Passphrase> retrieve(final OwnedBy ownedBy) throws UnableToRetrievePassphraseException {
        Objects.requireNonNull(ownedBy);
        final MasterKey masterKey = passphraseConfiguration.masterKey()
                .map(MasterKey::new)
                .orElseThrow(() -> new UnableToRetrievePassphraseException(
                        new IllegalStateException("Missing 'pulse.encryption-storage.master-key'")));
        final String ownerHash = hash(ownedBy);
        final String sql =
                // language=sql
                """
                        SELECT public.pgp_sym_decrypt(passphrase,?) as passphrase
                        FROM pulse.passphrase
                        WHERE owned_by_hashed = ?
                        """;
        try (final Connection connection = dataSource.get().getConnection();
             final PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, masterKey.key());
            stmt.setString(2, ownerHash);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Passphrase(rs.getString("passphrase").toCharArray()))
                        .map(passphraseObfuscator::obfuscate);
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
        final MasterKey masterKey = passphraseConfiguration.masterKey()
                .map(MasterKey::new)
                .orElseThrow(() -> new UnableToStorePassphraseException(
                        new IllegalStateException("Missing 'pulse.encryption-storage.master-key'")));
        final String ownerHash = hash(ownedBy);
        final String sql =
                // language=sql
                """
                        INSERT INTO pulse.passphrase(owned_by_hashed, passphrase)
                        VALUES (?, public.pgp_sym_encrypt(?::text,?))
                        """;
        try (final Connection connection = dataSource.get().getConnection();
             final PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ownerHash);
            stmt.setString(2, new String(passphrase.passphrase()));
            stmt.setString(3, masterKey.key());
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
