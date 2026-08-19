package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.encryption.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.apache.commons.codec.digest.DigestUtils.md5;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class OpenPGPDecryptionServiceTest {

    private OpenPGPEncryptionService encryptionService;
    private OpenPGPDecryptionService decryptionService;

    @Mock
    PassphraseProvider passphraseProvider;

    @BeforeEach
    void setUp() {
        encryptionService = new OpenPGPEncryptionService(passphraseProvider, new DefaultTemporaryPathProvider());
        decryptionService = new OpenPGPDecryptionService(passphraseProvider);
    }

    @Nested
    class ByteArray {

        @Test
        void shouldDecryptByOwnedBy() throws DecryptionException, UnableToProvidePassphraseException, EncryptionException {
            // Given
            final Encrypted<byte[]> encrypted = encryptionService.encrypt(new ByteArrayInputStream("Hello world!".getBytes(StandardCharsets.UTF_8)),
                    PassphraseSample.PASSPHRASE_1,
                    encryptedPayload -> {
                        try (final InputStream payload1 = encryptedPayload.payload()) {
                            return Encrypted.of(payload1.readAllBytes());
                        }
                    });
            doReturn(PassphraseSample.PASSPHRASE_1).when(passphraseProvider).provide(Todo.OWNED_BY_USER_1);

            // When
            final Decrypted<byte[]> decrypted = decryptionService.decrypt(encrypted, Todo.OWNED_BY_USER_1);

            // Then
            assertThat(decrypted.payload()).isEqualTo("Hello world!".getBytes(StandardCharsets.UTF_8));
        }

        // Meaning that the organization has been deleted from Vault ...
        @Test
        void shouldDecryptByOwnedByThrowUnknownPassphraseExceptionWhenPassphraseIsNotFound() throws UnableToProvidePassphraseException, EncryptionException {
            // Given
            final Encrypted<byte[]> encrypted = encryptionService.encrypt(new ByteArrayInputStream("Hello world!".getBytes(StandardCharsets.UTF_8)),
                    PassphraseSample.PASSPHRASE_1,
                    encryptedPayload -> {
                        try (final InputStream payload1 = encryptedPayload.payload()) {
                            return Encrypted.of(payload1.readAllBytes());
                        }
                    });
            doThrow(new UnableToProvidePassphraseException(new PassphraseBannedException()))
                    .when(passphraseProvider).provide(Todo.OWNED_BY_USER_1);

            // When && Then
            assertThatThrownBy(() -> decryptionService.decrypt(encrypted, Todo.OWNED_BY_USER_1))
                    .isExactlyInstanceOf(DecryptionException.class)
                    .cause()
                    .isExactlyInstanceOf(UnableToProvidePassphraseException.class)
                    .cause()
                    .isExactlyInstanceOf(PassphraseBannedException.class);
        }

        @Test
        void shouldDecryptByPassphrase() throws DecryptionException, EncryptionException {
            // Given
            final Encrypted<byte[]> encrypted = encryptionService.encrypt(new ByteArrayInputStream("Hello world!".getBytes(StandardCharsets.UTF_8)),
                    PassphraseSample.PASSPHRASE_1,
                    encryptedPayload -> {
                        try (final InputStream payload1 = encryptedPayload.payload()) {
                            return Encrypted.of(payload1.readAllBytes());
                        }
                    });

            // When
            final Decrypted<byte[]> decrypted = decryptionService.decrypt(encrypted, PassphraseSample.PASSPHRASE_1);

            // Then
            assertThat(decrypted.payload()).isEqualTo("Hello world!".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nested
    class File {

        // https://pixabay.com/sound-effects/search/tada/

        @Test
        void shouldEncryptNextDecrypt() throws Exception {
            // Given
            final String resourceName = "/floraphonic-tada-military-3-183975.wav";
            final byte[] givenResourceHash;
            try (final InputStream is = OpenPGPDecryptionServiceTest.class.getResourceAsStream(resourceName)) {
                assert is != null;
                givenResourceHash = md5(is);
            }

            // When
            final Encrypted<InputStream> encrypted = encryptionService.encrypt(
                    OpenPGPDecryptionServiceTest.class.getResourceAsStream(resourceName),
                    PassphraseSample.PASSPHRASE_1, t -> t);
            final Decrypted<InputStream> decrypted = decryptionService.decrypt(encrypted, PassphraseSample.PASSPHRASE_1,
                    t -> t);

            // Then
            try (final InputStream payload = decrypted.payload();
                 final ByteArrayInputStream reusable = new ByteArrayInputStream(payload.readAllBytes())) {
                assertArrayEquals(givenResourceHash, md5(reusable));
                reusable.reset();
                play(reusable);
            }
        }

        private void play(final InputStream inputStream) {
            Objects.requireNonNull(inputStream, "Audio input stream is null : " + inputStream);
            try (final AudioInputStream audioStream = AudioSystem.getAudioInputStream(inputStream);
                 final Clip clip = AudioSystem.getClip()) {
                clip.open(audioStream);
                clip.start();
                // Wait end of reading
                Thread.sleep(clip.getMicrosecondLength() / 1000);
            } catch (final UnsupportedAudioFileException | IOException | LineUnavailableException |
                           InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
