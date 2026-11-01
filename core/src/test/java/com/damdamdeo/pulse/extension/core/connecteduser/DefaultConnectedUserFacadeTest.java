package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.connectionidentifier.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Nested
    class IsRegistered {

        @Test
        void shouldReturnTrueWhenUserIsRegistered() throws Exception {
            // Given
            final ConnectionIdentifier givenConnectionIdentifier =
                    ConnectionIdentifier.from("abcdef123456");
            final AnyIdentifiable givenIdentifiable = mock(AnyIdentifiable.class);
            when(connectionIdentifierProvider.provide()).thenReturn(givenConnectionIdentifier);
            when(connectionIdentifierRepository.find(givenConnectionIdentifier)).thenReturn(Optional.of(givenIdentifiable));

            // When
            final Optional<AnyIdentifiable> connected = facade.isRegistered();

            // Then
            assertAll(
                    () -> assertEquals(Optional.of(givenIdentifiable), connected),
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
            final Optional<AnyIdentifiable> connected = facade.isRegistered();

            // Then
            assertAll(
                    () -> assertEquals(Optional.empty(), connected),
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
            final RegistrationCheckerException exception = assertThrows(
                    RegistrationCheckerException.class, () -> facade.isRegistered());

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
            final RegistrationCheckerException exception = assertThrows(
                    RegistrationCheckerException.class, () -> facade.isRegistered());

            // Then
            assertAll(
                    () -> assertSame(cause, exception.getCause()),
                    () -> verify(connectionIdentifierProvider).provide(),
                    () -> verify(connectionIdentifierRepository).find(givenConnectionIdentifier)
            );
        }
    }

    @Nested
    class GetConnectionIdentifier {

        @Test
        void shouldReturnConnectedUserWhenUserIsRegistered() throws Exception {
            // Given
            final ConnectionIdentifier givenConnectionIdentifier =
                    ConnectionIdentifier.from("abcdef123456");
            final AnyIdentifiable givenIdentifiable = mock(AnyIdentifiable.class);
            when(connectionIdentifierProvider.provide()).thenReturn(givenConnectionIdentifier);
            when(connectionIdentifierRepository.find(givenConnectionIdentifier)).thenReturn(Optional.of(givenIdentifiable));

            // When
            final AnyIdentifiable connected = facade.getConnectedUser();

            // Then
            assertAll(
                    () -> assertEquals(givenIdentifiable, connected),
                    () -> verify(connectionIdentifierProvider).provide(),
                    () -> verify(connectionIdentifierRepository).find(givenConnectionIdentifier)
            );
        }

        @Test
        void shouldThrowConnectedUserNotRegisteredExceptionWhenUserIsNotRegistered() throws Exception {
            // Given
            final ConnectionIdentifier givenConnectionIdentifier = ConnectionIdentifier.from("0000000000000000000000000000000000000000000000000000000000000000");
            when(connectionIdentifierProvider.provide()).thenReturn(givenConnectionIdentifier);
            when(connectionIdentifierRepository.find(givenConnectionIdentifier)).thenReturn(Optional.empty());

            // When / Then
            assertAll(
                    () -> assertThatThrownBy(() -> facade.getConnectedUser())
                            .isExactlyInstanceOf(ConnectedUserNotRegisteredException.class),
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
            final RegistrationCheckerException exception = assertThrows(
                    RegistrationCheckerException.class, () -> facade.getConnectedUser());

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
            final RegistrationCheckerException exception = assertThrows(
                    RegistrationCheckerException.class, () -> facade.getConnectedUser());

            // Then
            assertAll(
                    () -> assertSame(cause, exception.getCause()),
                    () -> verify(connectionIdentifierProvider).provide(),
                    () -> verify(connectionIdentifierRepository).find(givenConnectionIdentifier)
            );
        }
    }
}
