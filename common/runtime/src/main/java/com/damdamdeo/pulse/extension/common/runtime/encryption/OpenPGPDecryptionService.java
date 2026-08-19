package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.PBEDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;

import java.io.*;
import java.security.Security;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class OpenPGPDecryptionService implements DecryptionService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final PassphraseProvider passphraseProvider;

    public OpenPGPDecryptionService(final PassphraseProvider passphraseProvider) {
        this.passphraseProvider = Objects.requireNonNull(passphraseProvider);
    }

    @Override
    public <T> Decrypted<T> decrypt(final Encrypted<InputStream> encrypted,
                                    final OwnedBy ownedBy,
                                    final DecryptedInputStreamMapper<Decrypted<T>> mapper) throws DecryptionException {
        Objects.requireNonNull(encrypted);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(mapper);
        try {
            final Passphrase passphrase = passphraseProvider.provide(ownedBy);
            return decrypt(encrypted, passphrase, mapper);
        } catch (UnableToProvidePassphraseException exception) {
            throw new DecryptionException(exception);
        }
    }

    @Override
    public <T> Decrypted<T> decrypt(final Encrypted<InputStream> encrypted,
                                    final Passphrase passphrase,
                                    final DecryptedInputStreamMapper<Decrypted<T>> mapper) throws DecryptionException {
        Objects.requireNonNull(encrypted);
        Objects.requireNonNull(passphrase);
        Objects.requireNonNull(mapper);
        try {
            final PipedInputStream clearInput = new PipedInputStream(64 * 1024);
            final PipedOutputStream clearOutput = new PipedOutputStream(clearInput);

            final CompletableFuture<Void> future = new CompletableFuture<>();
            Thread.startVirtualThread(() -> {
                try (final InputStream encryptedInput = encrypted.payload();
                     clearOutput) {
                    decrypt(encryptedInput, clearOutput, passphrase);
                    future.complete(null);
                } catch (final Exception e) {
                    future.completeExceptionally(e);
                }
            });
            final InputStream decrypted = new FutureAwareInputStream(clearInput, future);
            return mapper.process(new Decrypted<>(decrypted));
        } catch (final IOException e) {
            throw new DecryptionException(e);
        }
    }

    @Override
    public Decrypted<byte[]> decrypt(final Encrypted<byte[]> encrypted, final OwnedBy ownedBy) throws DecryptionException {
        return decrypt(Encrypted.of(new ByteArrayInputStream(encrypted.payload())), ownedBy, decrypted -> {
            try (final InputStream payload = decrypted.payload()) {
                return new Decrypted<>(payload.readAllBytes());
            }
        });
    }

    @Override
    public Decrypted<byte[]> decrypt(final Encrypted<byte[]> encrypted, final Passphrase passphrase) throws DecryptionException {
        return decrypt(Encrypted.of(new ByteArrayInputStream(encrypted.payload())), passphrase, decrypted -> {
            try (final InputStream payload = decrypted.payload()) {
                return new Decrypted<>(payload.readAllBytes());
            }
        });
    }

    private void decrypt(final InputStream encrypted, final OutputStream clear, final Passphrase passphrase)
            throws IOException, PGPException {
        final PGPObjectFactory pgpFactory = new PGPObjectFactory(encrypted, new JcaKeyFingerprintCalculator());
        Object object = pgpFactory.nextObject();

        // Certains messages PGP commencent par un marqueur
        if (object instanceof PGPMarker) {
            object = pgpFactory.nextObject();
        }

        if (!(object instanceof PGPEncryptedDataList encryptedDataList)) {
            throw new PGPException("Invalid PGP structure");
        }

        final PGPPBEEncryptedData encryptedData = (PGPPBEEncryptedData) encryptedDataList.get(0);
        final PBEDataDecryptorFactory decryptorFactory =
                new JcePBEDataDecryptorFactoryBuilder(
                        new JcaPGPDigestCalculatorProviderBuilder().build())
                        .setProvider("BC")
                        .build(passphrase.passphrase());
        try (final InputStream decryptedStream = encryptedData.getDataStream(decryptorFactory)) {
            final PGPObjectFactory plainFactory =
                    new PGPObjectFactory(decryptedStream, new JcaKeyFingerprintCalculator());
            Object message = plainFactory.nextObject();
            if (message instanceof PGPCompressedData compressedData) {
                try (final InputStream compressedStream = compressedData.getDataStream()) {
                    message = new PGPObjectFactory(
                            compressedStream,
                            new JcaKeyFingerprintCalculator())
                            .nextObject();
                }
            }
            if (!(message instanceof PGPLiteralData literalData)) {
                throw new PGPException("Unexpected PGP content: expected PGPLiteralData");
            }
            try (final InputStream literalIn = literalData.getInputStream()) {
                final byte[] buffer = new byte[8192];
                int read;
                while ((read = literalIn.read(buffer)) != -1) {
                    clear.write(buffer, 0, read);
                }
                clear.flush();
            }
        }
        if (encryptedData.isIntegrityProtected() && !encryptedData.verify()) {
            throw new PGPException("PGP integrity check failed");
        }
    }
}
