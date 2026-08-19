package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.encryption.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class OpenPGPEncryptionServiceTest {

    private OpenPGPEncryptionService encryptionService;

    @Mock
    PassphraseProvider passphraseProvider;

    @Mock
    TemporaryPathProvider temporaryPathProvider;

    @BeforeEach
    void setUp() {
        encryptionService = new OpenPGPEncryptionService(passphraseProvider, temporaryPathProvider);
    }

    @Test
    void shouldEncryptByOwnedBy() throws EncryptionException, UnableToProvidePassphraseException, IOException {
        // Given
        final Path givenTempFile = Files.createTempFile("pulse-encrypted-", ".pgp");
        doReturn(givenTempFile).when(temporaryPathProvider).provide();

        final ByteArrayInputStream clearData = new ByteArrayInputStream("Hello world!".getBytes(StandardCharsets.UTF_8));
        doReturn(PassphraseSample.PASSPHRASE_1).when(passphraseProvider).provide(Todo.OWNED_BY_USER_1);

        // When
        final AtomicReference<Long> givenTempFileSize = new AtomicReference<>();
        final Encrypted<byte[]> encrypted = encryptionService.encrypt(clearData, Todo.OWNED_BY_USER_1,
                encryptedPayload -> {
                    givenTempFileSize.set(Files.size(givenTempFile));

                    try (final InputStream payload1 = encryptedPayload.payload()) {
                        return Encrypted.of(payload1.readAllBytes());
                    }
                });

        // Then
        clearData.reset();
        assertAll(
                () -> assertThat(givenTempFileSize.get()).isGreaterThan(1L),
                () -> assertThat(Files.exists(givenTempFile)).isFalse(),
                () -> assertThat(encrypted.payload()).isNotEmpty(),
                () -> assertThat(encrypted.payload()).isNotEqualTo(clearData.readAllBytes())
        );
    }

    @Test
    void shouldEncryptByOwnedByThrowUnknownPassphraseExceptionWhenPassphraseIsNotFound() throws UnableToProvidePassphraseException, IOException {
        // Given
        final ByteArrayInputStream clearData = new ByteArrayInputStream("Hello world!".getBytes(StandardCharsets.UTF_8));
        doThrow(new UnableToProvidePassphraseException(new PassphraseBannedException()))
                .when(passphraseProvider).provide(Todo.OWNED_BY_USER_1);

        // When && Then
        assertThatThrownBy(() -> encryptionService.encrypt(clearData, Todo.OWNED_BY_USER_1,
                encryptedPayload -> {
                    try (final InputStream payload1 = encryptedPayload.payload()) {
                        throw new IllegalStateException("Should not be called");
                    }
                }))
                .isExactlyInstanceOf(EncryptionException.class)
                .cause()
                .isExactlyInstanceOf(UnableToProvidePassphraseException.class)
                .cause()
                .isExactlyInstanceOf(PassphraseBannedException.class);
    }

    @Test
    void shouldEncryptByPassphrase() throws EncryptionException, IOException {
        // Given
        final Path givenTempFile = Files.createTempFile("pulse-encrypted-", ".pgp");
        doReturn(givenTempFile).when(temporaryPathProvider).provide();
        final ByteArrayInputStream clearData = new ByteArrayInputStream("Hello world!".getBytes(StandardCharsets.UTF_8));

        // When
        final Encrypted<byte[]> encrypted = encryptionService.encrypt(clearData, PassphraseSample.PASSPHRASE_1,
                encryptedPayload -> {
                    try (final InputStream payload1 = encryptedPayload.payload()) {
                        return Encrypted.of(payload1.readAllBytes());
                    }
                });

        // Then
        clearData.reset();
        assertAll(
                () -> assertThat(encrypted.payload()).isNotEmpty(),
                () -> assertThat(encrypted.payload()).isNotEqualTo(clearData.readAllBytes())
        );
    }
}
