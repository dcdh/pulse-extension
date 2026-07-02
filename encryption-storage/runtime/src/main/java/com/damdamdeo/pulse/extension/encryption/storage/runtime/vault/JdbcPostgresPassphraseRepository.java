package com.damdamdeo.pulse.extension.encryption.storage.runtime.vault;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Provider;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@ApplicationScoped
@Unremovable
@Transactional(value = TxType.MANDATORY)
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
        final MasterKey masterKey = retrieveMasterKey();
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
    public List<RetrievedPassphrase> retrieve(final List<OwnedBy> multiples) throws UnableToRetrievePassphraseException {
        Objects.requireNonNull(multiples);
        final MasterKey masterKey = retrieveMasterKey();
        final Map<OwnedBy, RetrievedPassphrase> retrievedPassphrases = new HashMap<>(multiples.size());
        final Map<Hash<OwnedBy>, OwnedBy> ownedByHash = new HashMap<>(multiples.size());
        final Map<OwnedBy, Hash<OwnedBy>> hashByOwnedBy = new HashMap<>(multiples.size());
        for (final OwnedBy ownedBy : multiples) {
            ownedByHash.put(hasher.hash(ownedBy), ownedBy);
            hashByOwnedBy.put(ownedBy, hasher.hash(ownedBy));
            retrievedPassphrases.put(ownedBy, new RetrievedPassphrase(ownedBy, null));
        }
        final String sql =
                // language=sql
                """
                        SELECT public.pgp_sym_decrypt(passphrase,?) as passphrase, owned_by_hashed as owned_by_hashed
                        FROM pulse.passphrase
                        WHERE owned_by_hashed = ANY(?::varchar[])
                        """;
        try (final Connection connection = dataSource.get().getConnection();
             final PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, masterKey.key());
            final Array eventsArray = connection.createArrayOf("varchar", multiples.stream()
                    .map(ownedBy -> hashByOwnedBy.get(ownedBy).value())
                    .toArray(String[]::new));
            stmt.setArray(2, eventsArray);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    final OwnedBy ownedByHashed = Objects.requireNonNull(ownedByHash.get(new Hash<OwnedBy>(rs.getString("owned_by_hashed"))));
                    retrievedPassphrases.put(ownedByHashed, new RetrievedPassphrase(ownedByHashed, new Passphrase(rs.getString("passphrase").toCharArray())));
                }
            }
            return new ArrayList<>(retrievedPassphrases.values());
        } catch (final SQLException sqlException) {
            throw new UnableToRetrievePassphraseException(sqlException);
        }
    }

    @Override
    public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException,
            UnableToStorePassphraseException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(passphrase);
        final MasterKey masterKey = retrieveMasterKey();
        final String ownerHash = hash(ownedBy);
        final String sql =
                // language=sql
                """
                        INSERT INTO pulse.passphrase(owned_by_hashed, passphrase)
                        VALUES (?, public.pgp_sym_encrypt(?::text,?))
                        ON CONFLICT (owned_by_hashed) DO NOTHING
                        """;
        try (final Connection connection = dataSource.get().getConnection();
             final PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ownerHash);
            stmt.setString(2, new String(passphrase.passphrase()));
            stmt.setString(3, masterKey.key());
            final int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new PassphraseAlreadyExistsException(ownedBy);
            }
            return new Passphrase(passphrase.passphrase().clone());
        } catch (final SQLException sqlException) {
            throw new UnableToStorePassphraseException(sqlException);
        }
    }

    private String hash(final OwnedBy ownedBy) {
        return hasher.hash(ownedBy).value();
    }

    private MasterKey retrieveMasterKey() {
        return passphraseConfiguration.masterKey()
                .map(MasterKey::new)
                .orElseThrow(() -> new IllegalStateException("Missing 'pulse.encryption-storage.master-key'"));
    }
}
