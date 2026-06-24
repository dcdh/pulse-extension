package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.TechnicalException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.*;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultConnectedUserFacadeTest {

    @Mock
    ConnectionIdentifierProvider connectionIdentifierProvider;

    @Mock
    ConnectionIdentifierRepository connectionIdentifierRepository;

    @InjectMocks
    DefaultConnectedUserFacade facade;

    @Test
    void shouldReturnTrueWhenUserIsRegistered() throws Exception {
        // Given
        final ConnectionIdentifier givenConnectionIdentifier =
                ConnectionIdentifier.from("abcdef123456");
        final Identifiable givenIdentifiable = mock(Identifiable.class);
        when(connectionIdentifierProvider.provide()).thenReturn(givenConnectionIdentifier);
        when(connectionIdentifierRepository.find(givenConnectionIdentifier)).thenReturn(Optional.of(givenIdentifiable));

        // When
        final boolean result = facade.isRegistered();

        // Then
        assertAll(
                () -> assertTrue(result),
                () -> verify(connectionIdentifierProvider).provide(),
                () -> verify(connectionIdentifierRepository).find(givenConnectionIdentifier)
        );
    }

    @Test
    void shouldReturnFalseWhenUserIsNotRegistered() throws Exception {
        // Given
        final ConnectionIdentifier givenConnectionIdentifier = ConnectionIdentifier.from("0000000000000000000000000000000000000000000000000000000000000000");
        when(connectionIdentifierProvider.provide()).thenReturn(givenConnectionIdentifier);
        when(connectionIdentifierRepository.find(givenConnectionIdentifier)).thenReturn(Optional.empty());

        // When
        final boolean result = facade.isRegistered();

        // Then
        assertAll(
                () -> assertFalse(result),
                () -> verify(connectionIdentifierProvider).provide(),
                () -> verify(connectionIdentifierRepository).find(givenConnectionIdentifier)
        );
    }

    @Test
    void shouldThrowTechnicalExceptionWhenConnectionIdentifierProviderFails() throws Exception {
        // Given
        final ConnectionIdentifierProviderException cause = new ConnectionIdentifierProviderException(new RuntimeException());
        when(connectionIdentifierProvider.provide()).thenThrow(cause);

        // When
        final TechnicalException exception = assertThrows(
                TechnicalException.class, () -> facade.isRegistered());

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(connectionIdentifierProvider).provide(),
                () -> verifyNoInteractions(connectionIdentifierRepository)
        );
    }

    @Test
    void shouldThrowTechnicalExceptionWhenConnectionIdentifierRepositoryFails() throws Exception {
        // Given
        final ConnectionIdentifier givenConnectionIdentifier = ConnectionIdentifier.from("0000000000000000000000000000000000000000000000000000000000000000");
        final ConnectionIdentifierRepositoryException cause = new ConnectionIdentifierRepositoryException("msg", new RuntimeException());
        when(connectionIdentifierProvider.provide()).thenReturn(givenConnectionIdentifier);
        when(connectionIdentifierRepository.find(givenConnectionIdentifier)).thenThrow(cause);

        // When
        final TechnicalException exception = assertThrows(
                TechnicalException.class, () -> facade.isRegistered());

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(connectionIdentifierProvider).provide(),
                () -> verify(connectionIdentifierRepository).find(givenConnectionIdentifier)
        );
    }
}
