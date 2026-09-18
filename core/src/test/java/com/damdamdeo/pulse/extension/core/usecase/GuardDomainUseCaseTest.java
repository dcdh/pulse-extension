package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CommandException;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.AggregateIdDecomposer;
import com.damdamdeo.pulse.extension.core.audience.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.audience.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.usecase.audience.Audience;
import com.damdamdeo.pulse.extension.core.usecase.audience.Everyone;
import com.damdamdeo.pulse.extension.core.usecase.audience.VisibilityRoleRestricted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardDomainUseCaseTest {

    private static final MarkTodoAsDone INPUT = new MarkTodoAsDone(TodoId.USER_1_TODO_1);

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;

    @Mock
    ExecutedByResolver executedByResolver;

    @Mock
    AggregateIdDecomposer aggregateIdDecomposer;

    @Mock
    CommandHandler<Todo, TodoId> commandHandler;

    final Supplier<MissingAggregateException> missingAggregateException = () -> {
        throw new RuntimeException("Should not be called");
    };

    StubDomainUseCase decorated;

    private GuardDomainUseCase<TodoId, MarkTodoAsDone, Todo> guardDomainUseCase;

    public static class StubDomainUseCase extends AbstractDomainUseCase<TodoId, MarkTodoAsDone, Todo> {

        final List<String> called = new ArrayList<>();
        private final List<Audience> audiences;
        private final Supplier<MissingAggregateException> missingAggregateException;

        protected StubDomainUseCase(final CommandHandler<Todo, TodoId> commandHandler,
                                    final List<Audience> audiences,
                                    final Supplier<MissingAggregateException> missingAggregateException) {
            super(commandHandler);
            this.audiences = audiences;
            this.missingAggregateException = missingAggregateException;
        }

        @Override
        protected MarkTodoAsDone onBefore(final MarkTodoAsDone command) throws UseCaseExecutionException {
            called.add("onBefore");
            return command;
        }

        @Override
        protected Todo onAfter(final MarkTodoAsDone command, final Todo aggregate) throws UseCaseExecutionException {
            called.add("onAfter");
            return super.onAfter(command, aggregate);
        }

        @Override
        protected Supplier<MissingAggregateException> missingAggregateException() {
            return missingAggregateException;
        }

        @Override
        public List<Audience> audiences() {
            called.add("audiences");
            return audiences;
        }

        public List<String> called() {
            return called;
        }
    }

    @Test
    void shouldReturnResultWhenEveryoneAllowsAccess() throws UseCaseException, CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(Everyone.INSTANCE), missingAggregateException);
        when(commandHandler.handle(INPUT, missingAggregateException)).thenReturn(new Todo(TodoId.USER_1_TODO_1));
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated) {
        };

        // When
        final Todo executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Todo(TodoId.USER_1_TODO_1), executed),
                () -> assertThat(decorated.called()).containsExactly("audiences", "onBefore", "onAfter"),
                () -> verify(commandHandler).handle(any(), any())
        );
    }

    @Test
    void shouldReturnResultFromFirstAudienceThatAllowsAccess() throws UseCaseException, CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(VisibilityRoleRestricted.INSTANCE, Everyone.INSTANCE), missingAggregateException);
        when(commandHandler.handle(INPUT, missingAggregateException)).thenReturn(new Todo(TodoId.USER_1_TODO_1));
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated) {
        };

        // When
        final Todo executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Todo(TodoId.USER_1_TODO_1), executed),
                () -> assertThat(decorated.called()).containsExactly("audiences", "onBefore", "onAfter"),
                () -> verify(commandHandler).handle(any(), any())
        );
    }

    @Test
    void shouldExecuteAudiencesInPriorityOrder() throws UseCaseException, CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(Everyone.INSTANCE, VisibilityRoleRestricted.INSTANCE), missingAggregateException);
        when(commandHandler.handle(INPUT, missingAggregateException)).thenReturn(new Todo(TodoId.USER_1_TODO_1));
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated) {
        };

        // When
        final Todo executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Todo(TodoId.USER_1_TODO_1), executed),
                () -> assertThat(decorated.called()).containsExactly("audiences", "onBefore", "onAfter"),
                () -> verify(commandHandler).handle(any(), any())
        );
    }

    @Test
    void shouldNotExecuteFollowingAudiencesWhenEveryoneAllowsAccess() throws UseCaseException, CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(Everyone.INSTANCE, VisibilityRoleRestricted.INSTANCE), missingAggregateException);
        when(commandHandler.handle(INPUT, missingAggregateException)).thenReturn(new Todo(TodoId.USER_1_TODO_1));
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated) {
        };

        // When
        final Todo executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Todo(TodoId.USER_1_TODO_1), executed),
                () -> assertThat(decorated.called()).containsExactly("audiences", "onBefore", "onAfter"),
                () -> verifyNoInteractions(executionContextProvider, backendUserVisibilityRolesProvider,
                        executedByResolver)
        );
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenNoAudienceAllowsAccess() {
        // Given
        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.ServiceAccount("backend"), Set.of("reader"));
        decorated = new StubDomainUseCase(commandHandler, List.of(VisibilityRoleRestricted.INSTANCE), missingAggregateException);
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated) {
        };
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("admin"));

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> guardDomainUseCase.execute(INPUT))
                        .isExactlyInstanceOf(UseCaseException.class)
                        .cause()
                        .isExactlyInstanceOf(UnauthorizedException.class),
                () -> assertThat(decorated.called()).containsExactly("audiences")
        );
    }

    @Test
    void shouldDelegateAudiences() {
        // Given
        final List<Audience> audiences = List.of(Everyone.INSTANCE, VisibilityRoleRestricted.INSTANCE);
        decorated = new StubDomainUseCase(commandHandler, audiences, missingAggregateException);
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated) {
        };

        // When
        final List<Audience> result = guardDomainUseCase.audiences();

        // Then
        assertSame(audiences, result);
    }
}
