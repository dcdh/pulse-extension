package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.connecteduser.*;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionIdentifierProviderTest {

    @Mock
    ConnectedUserProvider connectedUserProvider;

    @Mock
    Hasher hasher;

    @InjectMocks
    ConnectionIdentifierProvider connectionIdentifierProvider;

    @Test
    void shouldProvideConnectionIdentifier() throws Exception {
        // Given
        final ConnectedUser givenConnectedUser = new ConnectedUser(new Username("damien.clementdhuart@gmail.com"));
        when(connectedUserProvider.provide()).thenReturn(givenConnectedUser);
        when(hasher.hash(givenConnectedUser)).thenReturn(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"));

        // When
        final ConnectionIdentifier result = connectionIdentifierProvider.provide();

        // Then
        assertAll(
                () -> assertEquals(ConnectionIdentifier.from(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000")), result),
                () -> verify(connectedUserProvider).provide(),
                () -> verify(hasher).hash(givenConnectedUser)
        );
    }

    @Test
    void shouldThrowConnectionIdentifierProviderExceptionWhenConnectedIsAnonymousExceptionOccurs() throws Exception {
        // Given
        ConnectedIsAnonymousException givenCause = new ConnectedIsAnonymousException();
        when(connectedUserProvider.provide()).thenThrow(givenCause);

        // When
        final ConnectionIdentifierProviderException exception = assertThrows(
                ConnectionIdentifierProviderException.class, () -> connectionIdentifierProvider.provide());

        // Then
        assertAll(
                () -> assertSame(givenCause, exception.getCause()),
                () -> verify(connectedUserProvider).provide(),
                () -> verifyNoInteractions(hasher)
        );
    }

    @Test
    void shouldThrowConnectionIdentifierProviderExceptionWhenUsernameNotAMailExceptionOccurs() throws Exception {
        // Given
        final UsernameNotAMailException GivenCause = new UsernameNotAMailException();
        when(connectedUserProvider.provide()).thenThrow(GivenCause);

        // When
        final ConnectionIdentifierProviderException exception = assertThrows(
                ConnectionIdentifierProviderException.class, () -> connectionIdentifierProvider.provide());

        // Then
        assertAll(
                () -> assertSame(GivenCause, exception.getCause()),
                () -> verify(connectedUserProvider).provide(),
                () -> verifyNoInteractions(hasher)
        );
    }

    @Test
    void shouldThrowConnectionIdentifierProviderExceptionWhenConnectedUserNotAvailableExceptionOccurs() throws Exception {
        // Given
        ConnectedUserNotAvailableException givenCause = new ConnectedUserNotAvailableException();
        when(connectedUserProvider.provide()).thenThrow(givenCause);

        // When
        final ConnectionIdentifierProviderException exception = assertThrows(
                ConnectionIdentifierProviderException.class, () -> connectionIdentifierProvider.provide());

        // Then
        assertAll(
                () -> assertSame(givenCause, exception.getCause()),
                () -> verify(connectedUserProvider).provide(),
                () -> verifyNoInteractions(hasher)
        );
    }
}
