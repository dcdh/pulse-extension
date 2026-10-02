package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.permission.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.permission.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.query.AggregateIdDecomposer;
import com.damdamdeo.pulse.extension.core.traceability.ExecutionStatus;
import com.damdamdeo.pulse.extension.core.traceability.From;
import com.damdamdeo.pulse.extension.core.traceability.Source;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppender;
import com.damdamdeo.pulse.extension.core.usecase.permission.Everyone;
import com.damdamdeo.pulse.extension.core.usecase.permission.Permission;
import com.damdamdeo.pulse.extension.core.usecase.permission.VisibilityRoleRestricted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
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

    private static final MarkTodoAsDone INPUT_LOCKING_EXCEPTION = new MarkTodoAsDone(TodoId.USER_1_TODO_2);

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

    @Spy
    DistributedLockManager distributedLockManager = new DistributedLockManager() {

        @Override
        public <K extends AggregateId, C extends Command<K>, A extends AggregateRoot<K>> Handled<A, K> executeWithLock(final C command, final UseCaseExecutor<K, C, A> useCaseExecutor) throws LockingException, UseCaseException {
            if (INPUT_LOCKING_EXCEPTION.equals(command)) {
                throw new LockingException(new RuntimeException("BOOM"));
            }
            return useCaseExecutor.execute(command);
        }
    };

    @Mock
    TraceAppender traceAppender;

    final Supplier<MissingAggregateException> missingAggregateException = () -> {
        throw new RuntimeException("Should not be called");
    };

    StubDomainUseCase decorated;

    private GuardDomainUseCase<TodoId, MarkTodoAsDone, Todo> guardDomainUseCase;

    public static class StubDomainUseCase extends AbstractDomainUseCase<TodoId, MarkTodoAsDone, Todo> {

        final List<String> called = new ArrayList<>();
        private final List<Permission<TodoId, MarkTodoAsDone>> permissions;
        private final Supplier<MissingAggregateException> missingAggregateException;

        protected StubDomainUseCase(final CommandHandler<Todo, TodoId> commandHandler,
                                    final List<Permission<TodoId, MarkTodoAsDone>> permissions,
                                    final Supplier<MissingAggregateException> missingAggregateException) {
            super(commandHandler);
            this.permissions = permissions;
            this.missingAggregateException = missingAggregateException;
        }

        @Override
        protected MarkTodoAsDone onBefore(final MarkTodoAsDone command) throws UseCaseExecutionException {
            called.add("onBefore");
            return command;
        }

        @Override
        protected Handled<Todo, TodoId> onAfter(final MarkTodoAsDone command, final Handled<Todo, TodoId> handled) throws UseCaseExecutionException {
            called.add("onAfter");
            return super.onAfter(command, handled);
        }

        @Override
        protected Supplier<MissingAggregateException> missingAggregateException() {
            return missingAggregateException;
        }

        @Override
        public List<Permission<TodoId, MarkTodoAsDone>> permissions() {
            called.add("permissions");
            return permissions;
        }

        public List<String> called() {
            return called;
        }
    }

    @Test
    void shouldReturnResultWhenEveryoneAllowsAccess() throws UseCaseException, CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(new Everyone<>()), missingAggregateException);
        when(commandHandler.handle(INPUT, missingAggregateException)).thenReturn(new Handled<>(new Todo(TodoId.USER_1_TODO_1), List.of()));
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, distributedLockManager, traceAppender) {
        };

        // When
        final Handled<Todo, TodoId> executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Handled<>(new Todo(TodoId.USER_1_TODO_1), List.of()), executed),
                () -> assertThat(decorated.called()).containsExactly("permissions", "onBefore", "onAfter"),
                () -> verify(commandHandler).handle(any(), any()),
                () -> verify(traceAppender).append(new AggregateIdTraceable<>(TodoId.USER_1_TODO_1), Source.COMMAND,
                        ExecutionStatus.SUCCESS, From.from(INPUT)),
                () -> verify(distributedLockManager).executeWithLock(eq(INPUT), any(UseCaseExecutor.class))
        );
    }

    @Test
    void shouldReturnResultFromFirstAudienceThatAllowsAccess() throws UseCaseException, CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(new VisibilityRoleRestricted<>(), new Everyone<>()), missingAggregateException);
        when(commandHandler.handle(INPUT, missingAggregateException)).thenReturn(new Handled<>(new Todo(TodoId.USER_1_TODO_1), List.of()));
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, distributedLockManager, traceAppender) {
        };

        // When
        final Handled<Todo, TodoId> executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Handled<>(new Todo(TodoId.USER_1_TODO_1), List.of()), executed),
                () -> assertThat(decorated.called()).containsExactly("permissions", "onBefore", "onAfter"),
                () -> verify(commandHandler).handle(any(), any()),
                () -> verify(traceAppender).append(new AggregateIdTraceable<>(TodoId.USER_1_TODO_1), Source.COMMAND,
                        ExecutionStatus.SUCCESS, From.from(INPUT)),
                () -> verify(distributedLockManager).executeWithLock(eq(INPUT), any(UseCaseExecutor.class))
        );
    }

    @Test
    void shouldExecutePermissionsInPriorityOrder() throws UseCaseException, CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(new Everyone<>(), new VisibilityRoleRestricted<>()), missingAggregateException);
        when(commandHandler.handle(INPUT, missingAggregateException)).thenReturn(new Handled<>(new Todo(TodoId.USER_1_TODO_1), List.of()));
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, distributedLockManager, traceAppender) {
        };

        // When
        final Handled<Todo, TodoId> executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Handled<>(new Todo(TodoId.USER_1_TODO_1), List.of()), executed),
                () -> assertThat(decorated.called()).containsExactly("permissions", "onBefore", "onAfter"),
                () -> verify(commandHandler).handle(any(), any()),
                () -> verify(traceAppender).append(new AggregateIdTraceable<>(TodoId.USER_1_TODO_1), Source.COMMAND,
                        ExecutionStatus.SUCCESS, From.from(INPUT))
        );
    }

    @Test
    void shouldNotExecuteFollowingPermissionsWhenEveryoneAllowsAccess() throws UseCaseException, CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(new Everyone<>(), new VisibilityRoleRestricted<>()), missingAggregateException);
        when(commandHandler.handle(INPUT, missingAggregateException)).thenReturn(new Handled<>(new Todo(TodoId.USER_1_TODO_1), List.of()));
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, distributedLockManager, traceAppender) {
        };

        // When
        final Handled<Todo, TodoId> executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Handled<>(new Todo(TodoId.USER_1_TODO_1), List.of()), executed),
                () -> assertThat(decorated.called()).containsExactly("permissions", "onBefore", "onAfter"),
                () -> verify(traceAppender).append(new AggregateIdTraceable<>(TodoId.USER_1_TODO_1), Source.COMMAND,
                        ExecutionStatus.SUCCESS, From.from(INPUT)),
                () -> verifyNoInteractions(executionContextProvider, backendUserVisibilityRolesProvider,
                        executedByResolver),
                () -> verify(distributedLockManager).executeWithLock(eq(INPUT), any(UseCaseExecutor.class))
        );
    }

    @Test
    void shouldThrowUseCaseExceptionWhenNoAudienceAllowsAccess() {
        // Given
        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.ServiceAccount("backend"), Set.of("reader"));
        decorated = new StubDomainUseCase(commandHandler, List.of(new VisibilityRoleRestricted<>()), missingAggregateException);
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, distributedLockManager, traceAppender) {
        };
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("admin"));

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> guardDomainUseCase.execute(INPUT))
                        .isExactlyInstanceOf(UseCaseException.class)
                        .cause()
                        .isExactlyInstanceOf(UnauthorizedException.class),
                () -> assertThat(decorated.called()).containsExactly("permissions"),
                () -> verify(traceAppender).append(new AggregateIdTraceable<>(TodoId.USER_1_TODO_1), Source.COMMAND,
                        ExecutionStatus.FAILED_UNAUTHORIZED, From.from(INPUT)),
                () -> verifyNoInteractions(distributedLockManager)
        );
    }

    @Test
    void shouldThrowUseCaseExceptionWhenUseCaseExceptionFromBusinessException() throws CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(new Everyone<>(), new VisibilityRoleRestricted<>()), missingAggregateException);
        doThrow(new CommandException(new BusinessException(new RuntimeException("BOOM"))))
                .when(commandHandler).handle(INPUT, missingAggregateException);
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, distributedLockManager, traceAppender) {
        };

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> guardDomainUseCase.execute(INPUT))
                        .isExactlyInstanceOf(UseCaseException.class)
                        .cause()
                        .isExactlyInstanceOf(CommandException.class)
                        .cause()
                        .isExactlyInstanceOf(BusinessException.class),
                () -> assertThat(decorated.called()).containsExactly("permissions", "onBefore"),
                () -> verify(traceAppender).append(new AggregateIdTraceable<>(TodoId.USER_1_TODO_1), Source.COMMAND,
                        ExecutionStatus.FAILED_BUSINESS, From.from(INPUT)),
                () -> verify(distributedLockManager).executeWithLock(eq(INPUT), any(UseCaseExecutor.class))
        );
    }

    @Test
    void shouldThrowUseCaseExceptionFromLockingExceptionOnExecute() throws CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(new Everyone<>(), new VisibilityRoleRestricted<>()), missingAggregateException);
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, distributedLockManager, traceAppender) {
        };

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> guardDomainUseCase.execute(INPUT_LOCKING_EXCEPTION))
                        .isExactlyInstanceOf(UseCaseException.class)
                        .cause()
                        .isExactlyInstanceOf(LockingException.class)
                        .cause()
                        .isExactlyInstanceOf(RuntimeException.class)
                        .hasMessage("BOOM"),
                () -> assertThat(decorated.called()).containsExactly("permissions"),
                () -> verifyNoInteractions(traceAppender),
                () -> verify(distributedLockManager).executeWithLock(eq(INPUT_LOCKING_EXCEPTION), any(UseCaseExecutor.class))
        );
    }

    @Test
    void shouldDelegatePermissions() {
        // Given
        final List<Permission<TodoId, MarkTodoAsDone>> permissions = List.of(new Everyone<>(), new VisibilityRoleRestricted<>());
        decorated = new StubDomainUseCase(commandHandler, permissions, missingAggregateException);
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, distributedLockManager, traceAppender) {
        };

        // When
        final List<Permission<TodoId, MarkTodoAsDone>> result = guardDomainUseCase.permissions();

        // Then
        assertSame(permissions, result);
    }
}
