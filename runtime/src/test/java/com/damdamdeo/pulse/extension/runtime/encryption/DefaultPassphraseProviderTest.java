package com.damdamdeo.pulse.extension.runtime.encryption;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseGenerator;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultPassphraseProviderTest {

    private DefaultPassphraseProvider passphraseProvider;

    @Mock
    PassphraseRepository passphraseRepository;

    @Mock
    PassphraseGenerator passphraseGenerator;

    @BeforeEach
    void setUp() {
        passphraseProvider = new DefaultPassphraseProvider(passphraseRepository, passphraseGenerator);
    }

    @Test
    void shouldRetrievePassphrase() {
        // Given
        doReturn(Optional.of(PassphraseSample.PASSPHRASE)).when(passphraseRepository).retrieve(new OwnedBy("custom organization"));

        // When
        final Passphrase customOrganizations = passphraseProvider.provide(new OwnedBy("custom organization"));

        // Then
        assertThat(customOrganizations.passphrase()).isEqualTo("7-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&".toCharArray());
    }

    @Test
    void shouldReturnGeneratedPassphraseWhenRetrieveReturnEmpty() {
        // Given
        doReturn(Optional.empty()).when(passphraseRepository).retrieve(new OwnedBy("custom organization"));
        doReturn(PassphraseSample.PASSPHRASE).when(passphraseGenerator).generate();

        // When
        final Passphrase customOrganizations = passphraseProvider.provide(new OwnedBy("custom organization"));

        // Then
        assertThat(customOrganizations.passphrase()).isEqualTo("7-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&".toCharArray());
    }

    @Test
    void shouldGenerateANewPassphraseWhenRetrieveReturnEmpty() {
        // Given
        doReturn(Optional.empty()).when(passphraseRepository).retrieve(new OwnedBy("custom organization"));
        doReturn(PassphraseSample.PASSPHRASE).when(passphraseGenerator).generate();

        // When
        passphraseProvider.provide(new OwnedBy("custom organization"));

        // Then
        verify(passphraseGenerator, times(1)).generate();
    }

    @Test
    void shouldStoreGeneratedPassphraseWhenRetrieveReturnEmpty() {
        // Given
        doReturn(Optional.empty()).when(passphraseRepository).retrieve(new OwnedBy("custom organization"));
        doReturn(PassphraseSample.PASSPHRASE).when(passphraseGenerator).generate();

        // When
        passphraseProvider.provide(new OwnedBy("custom organization"));

        // Then
        verify(passphraseRepository, times(1)).store(new OwnedBy("custom organization"),
                new Passphrase("7-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&".toCharArray()));
    }
}
