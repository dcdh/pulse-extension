package com.damdamdeo.pulse.extension.core.connecteduser.registration;

import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.TechnicalException;
import com.damdamdeo.pulse.extension.core.User;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.RegisterUser;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connectionidentifier.*;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractRegistrationDomainUseCaseTest {

    @Mock
    CommandHandler<User, UserId> commandHandler;

    @Mock
    ConnectionIdentifierProvider connectionIdentifierProvider;

    @Mock
    ConnectionIdentifierRepository connectionIdentifierRepository;

    @InjectMocks
    UserRegistrationDomainUseCase useCase;

    @Test
    void shouldRegisterUser() throws Exception {
        // Given
        final RegisterUser command = new RegisterUser();
        final ConnectionIdentifier connectionIdentifier = ConnectionIdentifier.from(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"));

        final User user = new User(UserId.USER_1);
        when(connectionIdentifierProvider.provide()).thenReturn(connectionIdentifier);
        when(commandHandler.handle(any(Function.class), eq(command), any())).thenReturn(user);

        // When
        final User result = useCase.execute(command);

        // Then
        assertAll(
                () -> assertEquals(user, result),
                () -> verify(connectionIdentifierProvider).provide(),
                () -> verify(commandHandler).handle(any(Function.class), eq(command), any()),
                () -> verify(connectionIdentifierRepository)
                        .store(connectionIdentifier, user.id())
        );
    }

    @Test
    void shouldThrowTechnicalExceptionWhenConnectionIdentifierProviderFails() throws Exception {
        // Given
        final RegisterUser command = new RegisterUser();
        final ConnectionIdentifierProviderException cause = new ConnectionIdentifierProviderException(new RuntimeException());
        when(connectionIdentifierProvider.provide()).thenThrow(cause);

        // When
        final TechnicalException exception = assertThrows(TechnicalException.class, () -> useCase.execute(command));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(connectionIdentifierProvider).provide(),
                () -> verifyNoInteractions(commandHandler),
                () -> verifyNoInteractions(connectionIdentifierRepository)
        );
    }

    @Test
    void shouldThrowTechnicalExceptionWhenRepositoryStoreFails() throws Exception {
        // Given
        final RegisterUser command = new RegisterUser();
        final ConnectionIdentifier connectionIdentifier = ConnectionIdentifier.from(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"));
        final User user = new User(UserId.USER_1);
        final ConnectionIdentifierRepositoryException cause = new ConnectionIdentifierRepositoryException("msg", new RuntimeException());
        when(connectionIdentifierProvider.provide()).thenReturn(connectionIdentifier);
        when(commandHandler.handle(any(Function.class), eq(command), any())).thenReturn(user);
        doThrow(cause)
                .when(connectionIdentifierRepository)
                .store(connectionIdentifier, user.id());

        // When
        final TechnicalException exception = assertThrows(TechnicalException.class, () -> useCase.execute(command));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(connectionIdentifierRepository)
                        .store(connectionIdentifier, user.id())
        );
    }

    @Test
    void shouldThrowBusinessExceptionWhenConnectionIdentifierAlreadyExists() throws Exception {
        // Given
        final RegisterUser command = new RegisterUser();
        final ConnectionIdentifier connectionIdentifier = ConnectionIdentifier.from(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"));
        final User user = new User(UserId.USER_1);
        final DuplicateConnectionIdentifierException cause = new DuplicateConnectionIdentifierException("msg");
        when(connectionIdentifierProvider.provide()).thenReturn(connectionIdentifier);
        when(commandHandler.handle(any(Function.class), eq(command), any())).thenReturn(user);
        doThrow(cause)
                .when(connectionIdentifierRepository)
                .store(connectionIdentifier, user.id());

        // When
        final BusinessException exception = assertThrows(BusinessException.class, () -> useCase.execute(command));

        // Then
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldPropagateBusinessExceptionThrownByCommandHandler() throws Exception {
        // Given
        final RegisterUser command = new RegisterUser();
        final ConnectionIdentifier connectionIdentifier = ConnectionIdentifier.from(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"));
        final BusinessException cause = new BusinessException(new RuntimeException());
        when(connectionIdentifierProvider.provide()).thenReturn(connectionIdentifier);
        when(commandHandler.handle(any(Function.class), eq(command), any())).thenThrow(cause);

        // When / Then
        final BusinessException exception = assertThrows(BusinessException.class, () -> useCase.execute(command));

        assertSame(cause, exception);
    }
}
