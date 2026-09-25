package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.AggregateIdTraceable;
import com.damdamdeo.pulse.extension.core.command.CommandException;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.permission.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.permission.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.query.AggregateIdDecomposer;
import com.damdamdeo.pulse.extension.core.traceability.From;
import com.damdamdeo.pulse.extension.core.traceability.Source;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppender;
import com.damdamdeo.pulse.extension.core.usecase.permission.Everyone;
import com.damdamdeo.pulse.extension.core.usecase.permission.Permission;
import com.damdamdeo.pulse.extension.core.usecase.permission.VisibilityRoleRestricted;
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
        protected Todo onAfter(final MarkTodoAsDone command, final Todo aggregate) throws UseCaseExecutionException {
            called.add("onAfter");
            return super.onAfter(command, aggregate);
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

    // FCK
    @Test
    void shouldReturnResultWhenEveryoneAllowsAccess() throws UseCaseException, CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(new Everyone<>()), missingAggregateException);
        when(commandHandler.handle(INPUT, missingAggregateException)).thenReturn(new Todo(TodoId.USER_1_TODO_1));
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, traceAppender) {
        };

        // When
        final Todo executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Todo(TodoId.USER_1_TODO_1), executed),
                () -> assertThat(decorated.called()).containsExactly("permissions", "onBefore", "onAfter"),
                () -> verify(commandHandler).handle(any(), any()),
                () -> verify(traceAppender).append(new AggregateIdTraceable(TodoId.USER_1_TODO_1), Source.COMMAND, From.from(INPUT))
        );
    }

    @Test
    void shouldReturnResultFromFirstAudienceThatAllowsAccess() throws UseCaseException, CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(new VisibilityRoleRestricted<>(), new Everyone<>()), missingAggregateException);
        when(commandHandler.handle(INPUT, missingAggregateException)).thenReturn(new Todo(TodoId.USER_1_TODO_1));
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, traceAppender) {
        };

        // When
        final Todo executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Todo(TodoId.USER_1_TODO_1), executed),
                () -> assertThat(decorated.called()).containsExactly("permissions", "onBefore", "onAfter"),
                () -> verify(commandHandler).handle(any(), any()),
                () -> verify(traceAppender).append(new AggregateIdTraceable(TodoId.USER_1_TODO_1), Source.COMMAND, From.from(INPUT))
        );
    }

    @Test
    void shouldExecutePermissionsInPriorityOrder() throws UseCaseException, CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(new Everyone<>(), new VisibilityRoleRestricted<>()), missingAggregateException);
        when(commandHandler.handle(INPUT, missingAggregateException)).thenReturn(new Todo(TodoId.USER_1_TODO_1));
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, traceAppender) {
        };

        // When
        final Todo executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Todo(TodoId.USER_1_TODO_1), executed),
                () -> assertThat(decorated.called()).containsExactly("permissions", "onBefore", "onAfter"),
                () -> verify(commandHandler).handle(any(), any()),
                () -> verify(traceAppender).append(new AggregateIdTraceable(TodoId.USER_1_TODO_1), Source.COMMAND, From.from(INPUT))
        );
    }

    @Test
    void shouldNotExecuteFollowingPermissionsWhenEveryoneAllowsAccess() throws UseCaseException, CommandException {
        // Given
        decorated = new StubDomainUseCase(commandHandler, List.of(new Everyone<>(), new VisibilityRoleRestricted<>()), missingAggregateException);
        when(commandHandler.handle(INPUT, missingAggregateException)).thenReturn(new Todo(TodoId.USER_1_TODO_1));
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, traceAppender) {
        };

        // When
        final Todo executed = guardDomainUseCase.execute(INPUT);

        // Then
        assertAll(
                () -> assertEquals(new Todo(TodoId.USER_1_TODO_1), executed),
                () -> assertThat(decorated.called()).containsExactly("permissions", "onBefore", "onAfter"),
                () -> verify(traceAppender).append(new AggregateIdTraceable(TodoId.USER_1_TODO_1), Source.COMMAND, From.from(INPUT)),
                () -> verifyNoInteractions(executionContextProvider, backendUserVisibilityRolesProvider,
                        executedByResolver)
        );
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenNoAudienceAllowsAccess() {
        // Given
        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.ServiceAccount("backend"), Set.of("reader"));
        decorated = new StubDomainUseCase(commandHandler, List.of(new VisibilityRoleRestricted<>()), missingAggregateException);
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, traceAppender) {
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
                () -> verifyNoInteractions(traceAppender)
        );
    }

    @Test
    void shouldDelegatePermissions() {
        // Given
        final List<Permission<TodoId, MarkTodoAsDone>> permissions = List.of(new Everyone<>(), new VisibilityRoleRestricted<>());
        decorated = new StubDomainUseCase(commandHandler, permissions, missingAggregateException);
        guardDomainUseCase = new GuardDomainUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, aggregateIdDecomposer, decorated, traceAppender) {
        };

        // When
        final List<Permission<TodoId, MarkTodoAsDone>> result = guardDomainUseCase.permissions();

        // Then
        assertSame(permissions, result);
    }
}
