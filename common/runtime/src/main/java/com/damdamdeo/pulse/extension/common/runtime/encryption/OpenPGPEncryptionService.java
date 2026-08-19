package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Objects;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class OpenPGPEncryptionService implements EncryptionService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final PassphraseProvider passphraseProvider;
    private final TemporaryPathProvider temporaryPathProvider;

    public OpenPGPEncryptionService(final PassphraseProvider passphraseProvider,
                                    final TemporaryPathProvider temporaryPathProvider) {
        this.passphraseProvider = Objects.requireNonNull(passphraseProvider);
        this.temporaryPathProvider = Objects.requireNonNull(temporaryPathProvider);
    }

    @Override
    public <T> Encrypted<T> encrypt(final InputStream clearData, final OwnedBy ownedBy,
                                    final EncryptedInputStreamMapper<Encrypted<T>> mapper) throws EncryptionException {
        Objects.requireNonNull(clearData);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(mapper);
        try {
            final Passphrase passphrase = passphraseProvider.provide(ownedBy);
            return encrypt(clearData, passphrase, mapper);
        } catch (final UnableToProvidePassphraseException exception) {
            throw new EncryptionException(exception);
        }
    }

    @Override
    // Comply with 'pgp_sym_encrypt' used in Postgres
    public <T> Encrypted<T> encrypt(final InputStream clearData, final Passphrase passphrase,
                                    final EncryptedInputStreamMapper<Encrypted<T>> mapper) throws EncryptionException {
        Objects.requireNonNull(clearData);
        Objects.requireNonNull(passphrase);
        Objects.requireNonNull(mapper);
        try {
            final Path tempFile = temporaryPathProvider.provide();
            try {
                try (clearData;
                     final OutputStream encryptedOutput = Files.newOutputStream(tempFile)) {
                    encrypt(clearData, encryptedOutput, passphrase);
                }
                final long size = Files.size(tempFile);
                final InputStream encryptedInput = new DeleteOnCloseInputStream(
                        Files.newInputStream(tempFile), tempFile);
                return mapper.process(Encrypted.of(encryptedInput, size));
            } catch (final Exception e) {
                Files.deleteIfExists(tempFile);
                throw e;
            }
        } catch (final Exception e) {
            throw new EncryptionException(e);
        }
    }

    private void encrypt(final InputStream clearData, final OutputStream destination, final Passphrase passphrase)
            throws IOException, PGPException {
        final PGPEncryptedDataGenerator encGen =
                new PGPEncryptedDataGenerator(
                        new JcePGPDataEncryptorBuilder(PGPEncryptedData.AES_128)
                                .setWithIntegrityPacket(true)
                                .setSecureRandom(new SecureRandom())
                                .setProvider("BC"));
        encGen.addMethod(
                new JcePBEKeyEncryptionMethodGenerator(passphrase.passphrase())
                        .setProvider("BC"));
        try {
            try (final OutputStream encOut = encGen.open(destination, new byte[64 * 1024])) {
                final PGPLiteralDataGenerator literalGenerator = new PGPLiteralDataGenerator();
                try (final OutputStream literalOut = literalGenerator.open(
                        encOut,
                        PGPLiteralData.BINARY,
                        PGPLiteralData.CONSOLE,
                        new Date(),
                        new byte[64 * 1024])) {
                    final byte[] buffer = new byte[8192];
                    int read;
                    while ((read = clearData.read(buffer)) >= 0) {
                        literalOut.write(buffer, 0, read);
                    }
                } finally {
                    literalGenerator.close();
                }
            }
        } finally {
            encGen.close();
        }
    }
}
