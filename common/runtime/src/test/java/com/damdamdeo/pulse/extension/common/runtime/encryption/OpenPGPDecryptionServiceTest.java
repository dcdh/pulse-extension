package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.User;
import com.damdamdeo.pulse.extension.core.encryption.DecryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.encryption.UnknownPassphraseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class OpenPGPDecryptionServiceTest {

    private OpenPGPEncryptionService encryptionService;
    private OpenPGPDecryptionService decryptionService;

    @Mock
    PassphraseRepository passphraseRepository;

    @BeforeEach
    void setUp() {
        encryptionService = new OpenPGPEncryptionService();
        decryptionService = new OpenPGPDecryptionService(passphraseRepository);
    }

    @Test
    void shouldDecrypt() {
        // Given
        final EncryptedPayload encrypted = encryptionService.encrypt("Hello world!".getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE);
        doReturn(Optional.of(PassphraseSample.PASSPHRASE)).when(passphraseRepository).retrieve(User.OWNED_BY_USER_1);

        // When
        final DecryptedPayload decrypted = decryptionService.decrypt(encrypted, User.OWNED_BY_USER_1);

        // Then
        assertThat(decrypted).isEqualTo(new DecryptedPayload("Hello world!".getBytes(StandardCharsets.UTF_8)));
    }

    // Meaning that the organization has been deleted from Vault ...
    @Test
    void shouldThrowUnknownPassphraseExceptionWhenPassphraseIsNotFound() {
        // Given
        final EncryptedPayload encrypted = encryptionService.encrypt("Hello world!".getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE);
        doReturn(Optional.empty()).when(passphraseRepository).retrieve(User.OWNED_BY_USER_1);

        // When && Then
        assertThatThrownBy(() -> decryptionService.decrypt(encrypted, User.OWNED_BY_USER_1))
                .isExactlyInstanceOf(UnknownPassphraseException.class)
                .hasFieldOrPropertyWithValue("ownedBy", User.OWNED_BY_USER_1);
    }
}
