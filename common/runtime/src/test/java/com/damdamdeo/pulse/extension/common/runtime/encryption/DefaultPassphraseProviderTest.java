package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.TodoId;
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
        doReturn(Optional.of(PassphraseSample.PASSPHRASE)).when(passphraseRepository).retrieve(OwnedBy.from(TodoId.USER_1_TODO_1));

        // When
        final Passphrase customOrganizations = passphraseProvider.provide(OwnedBy.from(TodoId.USER_1_TODO_1));

        // Then
        assertThat(customOrganizations.passphrase()).isEqualTo("7-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&".toCharArray());
    }

    @Test
    void shouldReturnGeneratedPassphraseWhenRetrieveReturnEmpty() {
        // Given
        doReturn(Optional.empty()).when(passphraseRepository).retrieve(any());
        doReturn(PassphraseSample.PASSPHRASE).when(passphraseGenerator).generate();

        // When
        final Passphrase customOrganizations = passphraseProvider.provide(OwnedBy.from(TodoId.USER_1_TODO_1));

        // Then
        assertThat(customOrganizations.passphrase()).isEqualTo("7-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&".toCharArray());
    }

    @Test
    void shouldGenerateANewPassphraseWhenRetrieveReturnEmpty() {
        // Given
        doReturn(Optional.empty()).when(passphraseRepository).retrieve(any());
        doReturn(PassphraseSample.PASSPHRASE).when(passphraseGenerator).generate();

        // When
        passphraseProvider.provide(OwnedBy.from(TodoId.USER_1_TODO_1));

        // Then
        verify(passphraseGenerator, times(1)).generate();
    }

    @Test
    void shouldStoreGeneratedPassphraseWhenRetrieveReturnEmpty() {
        // Given
        doReturn(Optional.empty()).when(passphraseRepository).retrieve(any());
        doReturn(PassphraseSample.PASSPHRASE).when(passphraseGenerator).generate();

        // When
        passphraseProvider.provide(OwnedBy.from(TodoId.USER_1_TODO_1));

        // Then
        verify(passphraseRepository, times(1)).store(OwnedBy.from(TodoId.USER_1_TODO_1),
                new Passphrase("7-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&".toCharArray()));
    }
}
