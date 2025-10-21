package com.damdamdeo.pulse.extension.runtime.encryption;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class OpenPGPDecryptionServiceTest {

    private OpenPGPDecryptionService decryptionService;

    @Mock
    PassphraseRepository passphraseRepository;

    @BeforeEach
    void setUp() {
        decryptionService = new OpenPGPDecryptionService(passphraseRepository);
    }

    @Test
    void shouldDecrypt() {
        // Given
        final byte[] encrypted = encrypt("Hello world!".getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE);
        doReturn(Optional.of(PassphraseSample.PASSPHRASE)).when(passphraseRepository).retrieve(new OwnedBy("custom organization"));

        // When
        final byte[] decrypted = decryptionService.decrypt(encrypted, new OwnedBy("custom organization"));

        // Then
        assertThat(decrypted).isEqualTo("Hello world!".getBytes(StandardCharsets.UTF_8));
    }

    // Meaning that the organization has been deleted from Vault ...
    @Test
    void shouldThrowDecryptionExceptionWhenPassphraseIsNotFound() {
        // Given
        final byte[] encrypted = encrypt("Hello world!".getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE);
        doReturn(Optional.empty()).when(passphraseRepository).retrieve(new OwnedBy("custom organization"));

        // When && Then
        assertThatThrownBy(() -> decryptionService.decrypt(encrypted, new OwnedBy("custom organization")))
                .isExactlyInstanceOf(DecryptionException.class)
                .hasMessage("Unknown passphrase");
    }

    // Comply with 'pgp_sym_encrypt' used in Postgres
    public static byte[] encrypt(final byte[] clearData, final char[] passphrase) {
        Objects.requireNonNull(clearData);
        Objects.requireNonNull(passphrase);
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
                    new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
                            .setWithIntegrityPacket(true)
                            .setSecureRandom(new SecureRandom())
                            .setProvider("BC"));
            encGen.addMethod(new JcePBEKeyEncryptionMethodGenerator(passphrase).setProvider("BC"));

            try (final OutputStream encOut = encGen.open(out, new byte[1 << 16])) {
                final PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
                try (OutputStream literalOut = lData.open(encOut, PGPLiteralData.BINARY, "data", clearData.length, new Date())) {
                    literalOut.write(clearData);
                }
            }
            encGen.close();
            return out.toByteArray();
        } catch (IOException | PGPException e) {
            throw new RuntimeException(e);
        }
    }
}
