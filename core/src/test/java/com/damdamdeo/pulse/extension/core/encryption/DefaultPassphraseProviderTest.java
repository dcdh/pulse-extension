package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultPassphraseProviderTest {

    @Mock
    PassphraseRepository repository;

    @Mock
    PassphraseGenerator generator;

    @InjectMocks
    DefaultPassphraseProvider provider;

    @Test
    void shouldReturnExistingPassphraseWhenAlreadyPresent() throws Exception {
        // Given
        final Passphrase existing = PassphraseSample.PASSPHRASE_1;
        when(repository.findBy(Todo.OWNED_BY_USER_1)).thenReturn(Optional.of(existing));

        // When
        final Passphrase result = provider.provide(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertEquals(existing, result),
                () -> verify(repository).findBy(Todo.OWNED_BY_USER_1),
                () -> verifyNoInteractions(generator),
                () -> verify(repository, never()).store(any(), any()));
    }

    @Test
    void shouldGenerateAndStorePassphraseWhenNotFound() throws Exception {
        // Given
        final Passphrase generated = PassphraseSample.PASSPHRASE_1;
        when(repository.findBy(Todo.OWNED_BY_USER_1)).thenReturn(Optional.empty());
        when(generator.generate()).thenReturn(generated);
        when(repository.store(Todo.OWNED_BY_USER_1, generated)).thenReturn(generated);

        // When
        final Passphrase result = provider.provide(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertEquals(generated, result),
                () -> verify(repository).findBy(Todo.OWNED_BY_USER_1),
                () -> verify(generator).generate(),
                () -> verify(repository).store(Todo.OWNED_BY_USER_1, generated));
    }

    @Test
    void shouldThrowWhenExistingPassphraseIsBanned() throws Exception {
        // Given
        when(repository.findBy(Todo.OWNED_BY_USER_1))
                .thenReturn(Optional.of(new Passphrase(null)));

        // When && Then
        assertAll(
                () -> assertThrows(UnableToProvidePassphraseException.class, () -> provider.provide(Todo.OWNED_BY_USER_1)),
                () -> verifyNoInteractions(generator),
                () -> verify(repository, never()).store(any(), any()));
    }

    @Test
    void shouldThrowWhenRepositoryFindFails() throws Exception {
        // Given
        when(repository.findBy(Todo.OWNED_BY_USER_1))
                .thenThrow(new UnableToRetrievePassphraseException(new RuntimeException()));

        // When && Then
        assertAll(
                () -> assertThrows(UnableToProvidePassphraseException.class, () -> provider.provide(Todo.OWNED_BY_USER_1)),
                () -> verifyNoInteractions(generator));
    }

    @Test
    void shouldThrowWhenStoreFails() throws Exception {
        // Given
        final Passphrase generated = Passphrase.ofValid("12345678901234567890123456789012".toCharArray());
        when(repository.findBy(Todo.OWNED_BY_USER_1)).thenReturn(Optional.empty());
        when(generator.generate()).thenReturn(generated);
        when(repository.store(Todo.OWNED_BY_USER_1, generated))
                .thenThrow(new UnableToStorePassphraseException(new RuntimeException()));

        // When && Then
        assertThrows(
                UnableToProvidePassphraseException.class,
                () -> provider.provide(Todo.OWNED_BY_USER_1)
        );
    }

    @Test
    void shouldThrowWhenPassphraseAlreadyExistsDuringStore() throws Exception {
        // Given
        final Passphrase generated = PassphraseSample.PASSPHRASE_1;
        when(repository.findBy(Todo.OWNED_BY_USER_1)).thenReturn(Optional.empty());
        when(generator.generate()).thenReturn(generated);
        when(repository.store(Todo.OWNED_BY_USER_1, generated))
                .thenThrow(new PassphraseAlreadyExistsException(Todo.OWNED_BY_USER_1));

        // When && Then
        assertThrows(
                UnableToProvidePassphraseException.class,
                () -> provider.provide(Todo.OWNED_BY_USER_1));
    }

    @Test
    void shouldBanExistingPassphrase() throws Exception {
        // Given
        final Passphrase existing = PassphraseSample.PASSPHRASE_1;
        final Passphrase banned = existing.ban();
        when(repository.get(Todo.OWNED_BY_USER_1)).thenReturn(existing);
        when(repository.update(Todo.OWNED_BY_USER_1, banned)).thenReturn(banned);

        // When
        final Passphrase result = provider.ban(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertEquals(Status.BANNED, result.status()),
                () -> verify(repository).get(Todo.OWNED_BY_USER_1),
                () -> verify(repository).update(Todo.OWNED_BY_USER_1, banned));
    }

    @Test
    void shouldThrowWhenUnknownPassphraseDuringBan() throws Exception {
        // Given

        // When
        when(repository.get(Todo.OWNED_BY_USER_1))
                .thenThrow(new UnknownPassphraseException(Todo.OWNED_BY_USER_1));

        // Then
        assertAll(
                () -> assertThrows(
                        UnableToBanPassphraseException.class,
                        () -> provider.ban(Todo.OWNED_BY_USER_1)),
                () -> verify(repository, never()).update(any(), any()));
    }

    @Test
    void shouldThrowWhenRetrieveFailsDuringBan() throws Exception {
        // Given
        when(repository.get(Todo.OWNED_BY_USER_1))
                .thenThrow(new UnableToRetrievePassphraseException(new RuntimeException()));

        // When && Then
        assertAll(
                () -> assertThrows(
                        UnableToBanPassphraseException.class,
                        () -> provider.ban(Todo.OWNED_BY_USER_1)
                ),
                () -> verify(repository, never()).update(any(), any()));
    }

    @Test
    void shouldThrowWhenUpdateFailsDuringBan() throws Exception {
        // Given
        final Passphrase existing = PassphraseSample.PASSPHRASE_1;
        when(repository.get(Todo.OWNED_BY_USER_1)).thenReturn(existing);
        when(repository.update(Todo.OWNED_BY_USER_1, existing.ban()))
                .thenThrow(new UnableToStorePassphraseException(new RuntimeException()));

        // When && Then
        assertThrows(
                UnableToBanPassphraseException.class,
                () -> provider.ban(Todo.OWNED_BY_USER_1));
    }

    @Test
    void shouldFailWhenGeneratorReturnsBannedPassphrase() throws Exception {
        // Given
        when(repository.findBy(Todo.OWNED_BY_USER_1)).thenReturn(Optional.empty());
        when(generator.generate()).thenReturn(new Passphrase(null));

        // When && Then
        assertAll(
                () -> assertThrows(
                        IllegalStateException.class,
                        () -> provider.provide(Todo.OWNED_BY_USER_1)
                ),
                () -> verify(repository, never()).store(any(), any())
        );
    }
}
