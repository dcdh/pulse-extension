package com.damdamdeo.pulse.extension.core.connecteduser.update;

import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.TechnicalException;
import com.damdamdeo.pulse.extension.core.User;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.UserUpdateUsername;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connectionidentifier.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractUpdateUserNameUseCaseTest {

    @Mock
    CommandHandler<User, UserId> commandHandler;

    @Mock
    ConnectionIdentifierProvider connectionIdentifierProvider;

    @Mock
    ConnectionIdentifierRepository connectionIdentifierRepository;

    @InjectMocks
    UserUpdateUserNameUseCase useCase;

    @Test
    void shouldUpdateUsernameSuccessfully() throws Exception {
        // Given
        final UserUpdateUsername command = new UserUpdateUsername(UserId.USER_1);
        final ConnectionIdentifier connectionIdentifier = ConnectionIdentifier.from("abcdef123456");
        final User user = new User(UserId.USER_1);
        when(connectionIdentifierProvider.provide()).thenReturn(connectionIdentifier);
        when(commandHandler.handle(eq(command), any())).thenReturn(user);

        // When
        final User result = useCase.execute(command);

        // Then
        assertAll(
                () -> assertEquals(user, result),
                () -> verify(connectionIdentifierProvider).provide(),
                () -> verify(commandHandler).handle(eq(command), any()),
                () -> verify(connectionIdentifierRepository).store(connectionIdentifier, user.id())
        );
    }

    @Test
    void shouldThrowTechnicalExceptionWhenConnectionIdentifierProviderFails() throws Exception {
        // Given
        final UserUpdateUsername command = new UserUpdateUsername(UserId.USER_1);
        final ConnectionIdentifierProviderException cause = new ConnectionIdentifierProviderException(new RuntimeException());
        when(connectionIdentifierProvider.provide()).thenThrow(cause);

        // When
        final TechnicalException exception = assertThrows(TechnicalException.class, () -> useCase.execute(command));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verifyNoInteractions(commandHandler),
                () -> verifyNoInteractions(connectionIdentifierRepository)
        );
    }

    @Test
    void shouldThrowTechnicalExceptionWhenRepositoryFails() throws Exception {
        // Given
        final UserUpdateUsername command = new UserUpdateUsername(UserId.USER_1);
        final ConnectionIdentifier connectionIdentifier = ConnectionIdentifier.from("abcdef123456");
        final User user = new User(UserId.USER_1);
        final ConnectionIdentifierRepositoryException cause = new ConnectionIdentifierRepositoryException("msg", new RuntimeException());
        when(connectionIdentifierProvider.provide()).thenReturn(connectionIdentifier);
        when(commandHandler.handle(eq(command), any())).thenReturn(user);
        doThrow(cause)
                .when(connectionIdentifierRepository)
                .store(connectionIdentifier, user.id());

        // When
        final TechnicalException exception = assertThrows(TechnicalException.class,() -> useCase.execute(command));

        // Then
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldPropagateBusinessExceptionFromCommandHandler() throws Exception {
        // Given
        final UserUpdateUsername command = new UserUpdateUsername(UserId.USER_1);
        final BusinessException cause =new BusinessException(new RuntimeException());
        when(connectionIdentifierProvider.provide()).thenReturn(ConnectionIdentifier.from("abcdef123456"));
        when(commandHandler.handle(eq(command), any())).thenThrow(cause);

        // When
        final BusinessException exception = assertThrows(BusinessException.class, () -> useCase.execute(command));

        // Then
        assertSame(cause, exception);
    }
}
