package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.encryption.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        encryptionService = new OpenPGPEncryptionService();
        decryptionService = new OpenPGPDecryptionService(passphraseProvider);
    }

    @Test
    void shouldDecrypt() throws DecryptionException, UnableToProvidePassphraseException {
        // Given
        final EncryptedPayload encrypted = encryptionService.encrypt("Hello world!".getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE_1);
        doReturn(PassphraseSample.PASSPHRASE_1).when(passphraseProvider).provide(Todo.OWNED_BY_USER_1);

        // When
        final DecryptedPayload decrypted = decryptionService.decrypt(encrypted, Todo.OWNED_BY_USER_1);

        // Then
        assertThat(decrypted).isEqualTo(new DecryptedPayload("Hello world!".getBytes(StandardCharsets.UTF_8)));
    }

    // Meaning that the organization has been deleted from Vault ...
    @Test
    void shouldThrowUnknownPassphraseExceptionWhenPassphraseIsNotFound() throws UnableToProvidePassphraseException {
        // Given
        final EncryptedPayload encrypted = encryptionService.encrypt("Hello world!".getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE_1);
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
}
