package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.AggregateIdDecomposer;
import com.damdamdeo.pulse.extension.core.query.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.query.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.usecase.audience.Audience;
import com.damdamdeo.pulse.extension.core.usecase.audience.Everyone;
import com.damdamdeo.pulse.extension.core.usecase.audience.VisibilityRoleRestricted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardDomainUseCaseTest {

    private static final CreateTodo INPUT = new CreateTodo("lorem ipsum");

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;

    @Mock
    ExecutedByResolver executedByResolver;

    @Mock
    AggregateIdDecomposer aggregateIdDecomposer;

    @Mock
    DomainUseCase<TodoId, CreateTodo, Todo> decorated;

    private GuardDomainUseCase<TodoId, CreateTodo, Todo> guardDomainUseCase;

    @BeforeEach
    void setUp() {
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated) {
        };
    }

    @Test
    void shouldReturnResultWhenEveryoneAllowsAccess() throws UseCaseException {
        // Given
        when(decorated.audiences()).thenReturn(List.of(Everyone.INSTANCE));
        when(decorated.execute(INPUT)).thenReturn(new Todo(TodoId.USER_1_TODO_1));

        // When
        final Todo executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Todo(TodoId.USER_1_TODO_1), executed),
                () -> verify(decorated).execute(INPUT)
        );
    }

    @Test
    void shouldReturnResultFromFirstAudienceThatAllowsAccess() throws UseCaseException {
        // Given
        final VisibilityRoleRestricted visibilityRoleRestricted = VisibilityRoleRestricted.INSTANCE;
        when(decorated.audiences()).thenReturn(List.of(visibilityRoleRestricted, Everyone.INSTANCE));
        when(decorated.execute(INPUT)).thenReturn(new Todo(TodoId.USER_1_TODO_1));

        // When
        final Todo executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Todo(TodoId.USER_1_TODO_1), executed),
                () -> verify(decorated).execute(INPUT)
        );
    }

    @Test
    void shouldExecuteAudiencesInPriorityOrder() throws UseCaseException {
        // Given
        final VisibilityRoleRestricted visibilityRoleRestricted = VisibilityRoleRestricted.INSTANCE;
        when(decorated.audiences()).thenReturn(List.of(Everyone.INSTANCE, visibilityRoleRestricted));
        when(decorated.execute(INPUT)).thenReturn(new Todo(TodoId.USER_1_TODO_1));

        // When
        final Todo executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Todo(TodoId.USER_1_TODO_1), executed),
                () -> verify(decorated).execute(INPUT)
        );
    }

    @Test
    void shouldNotExecuteFollowingAudiencesWhenEveryoneAllowsAccess() throws UseCaseException {
        // Given
        when(decorated.audiences()).thenReturn(List.of(Everyone.INSTANCE, VisibilityRoleRestricted.INSTANCE));
        when(decorated.execute(INPUT)).thenReturn(new Todo(TodoId.USER_1_TODO_1));

        // When
        final Todo executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Todo(TodoId.USER_1_TODO_1), executed),
                () -> verify(decorated).execute(INPUT),
                () -> verifyNoInteractions(executionContextProvider, backendUserVisibilityRolesProvider,
                        executedByResolver)
        );
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenNoAudienceAllowsAccess() {
        // Given
        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.ServiceAccount("backend"), Set.of("reader"));
        when(decorated.audiences()).thenReturn(List.of(VisibilityRoleRestricted.INSTANCE));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("admin"));

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> guardDomainUseCase.execute(INPUT))
                        .isExactlyInstanceOf(UseCaseException.class)
                        .cause()
                        .isExactlyInstanceOf(UnauthorizedException.class),
                () -> verify(decorated).audiences()
        );
    }

    @Test
    void shouldDelegateAudiences() {
        // Given
        final List<Audience> audiences = List.of(Everyone.INSTANCE, VisibilityRoleRestricted.INSTANCE);
        when(decorated.audiences()).thenReturn(audiences);

        // When
        final List<Audience> result = guardDomainUseCase.audiences();

        // Then
        assertSame(audiences, result);
    }
}
