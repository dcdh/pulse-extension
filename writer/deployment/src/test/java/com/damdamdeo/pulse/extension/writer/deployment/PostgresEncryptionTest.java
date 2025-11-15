package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.EventStoreException;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PostgresEncryptionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .withConfigurationResource("application.properties");

    @Inject
    DecryptionService decryptionService;

    @Inject
    EncryptionService encryptionService;

    @Inject
    DataSource dataSource;

    @ApplicationScoped
    static class StubPassphraseRepository implements PassphraseRepository {

        @Override
        public Optional<Passphrase> retrieve(final OwnedBy ownedBy) {
            return Optional.of(PassphraseSample.PASSPHRASE);
        }

        @Override
        public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
            throw new IllegalStateException("Should not be called !");
        }
    }

    // docker kill $(docker ps -q)
    // docker rm $(docker ps -a -q)
    // psql -U quarkus quarkus
    // "\dn+" liste schemas
    // "\l" liste databases
    // "\c quarkus" le name est là
    // "\dt" lister les tables
    // SELECT pgp_sym_encrypt('Hello world!','passphrase') AS encrypted;
    @Test
    void shouldDecryptEncryptedValueFromPostgresUsingDecryptionService() {
        // Given
        byte[] encrypted;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement encryptedPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT pgp_sym_encrypt(?,?) AS encrypted
                             """
             )) {
            connection.setAutoCommit(false);
            encryptedPreparedStatement.setString(1, "Hello world!");
            encryptedPreparedStatement.setString(2, new String(PassphraseSample.PASSPHRASE.passphrase()));
            try (final ResultSet encryptedResultSet = encryptedPreparedStatement.executeQuery()) {
                encryptedResultSet.next();
                encrypted = encryptedResultSet.getBytes(1);
            }
        } catch (final SQLException e) {
            throw new EventStoreException(e);
        }

        // When
        final DecryptedPayload decrypted = decryptionService.decrypt(new EncryptedPayload(encrypted), new OwnedBy("Damien"));

        // Then
        assertAll(
                /*
pgp_sym_encrypt() implements the OpenPGP specification (RFC 4880), which requires the addition of random values (called salt and initialization vector, or IV) to each encryption.
These values ensure that:
- two identical messages encrypted with the same key produce different results,
- it is impossible to deduce the content or repetition of a message from the ciphertext,
- and that the encryption remains semantically secure.
                 */
//                () -> assertThat(encryptedAsString).isEqualTo("\\xc30d0407030231654111a015268367d23d01657e8a31b08aad73346bc8cf7061cab608eb7a880e80bc967292b8699345cc86f08a89a1afe228c97c21429f9b77517730b056c4669c9a4caeabb147"),
                () -> assertThat(decrypted).isEqualTo(new DecryptedPayload("Hello world!".getBytes(StandardCharsets.UTF_8)))
        );
    }

    @Test
    void shouldDecryptEncryptedValueFromDecryptionServiceUsingPostgres() {
        // Given
        final String givenToEncrypt = "Hello world!";

        // When
        final EncryptedPayload encrypted = encryptionService.encrypt(givenToEncrypt.getBytes(StandardCharsets.UTF_8),
                PassphraseSample.PASSPHRASE);

        byte[] decrypted;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement encryptedPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT pgp_sym_decrypt_bytea(?, ?) AS decrypted
                             """
             )) {
            connection.setAutoCommit(false);
            encryptedPreparedStatement.setBytes(1, encrypted.payload());
            encryptedPreparedStatement.setString(2, new String(PassphraseSample.PASSPHRASE.passphrase()));
            try (final ResultSet decryptedResultSet = encryptedPreparedStatement.executeQuery()) {
                decryptedResultSet.next();
                decrypted = decryptedResultSet.getBytes(1);
            }
        } catch (final SQLException e) {
            throw new EventStoreException(e);
        }

        // Then
        assertAll(
                () -> assertThat(decrypted).isEqualTo("Hello world!".getBytes(StandardCharsets.UTF_8))
        );
    }
}
