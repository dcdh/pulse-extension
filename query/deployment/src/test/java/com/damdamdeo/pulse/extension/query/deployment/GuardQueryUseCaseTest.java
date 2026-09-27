package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.query.permission.Permission;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardQueryUseCaseTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubPassphraseProvider.class,
                    TodoProjection.class, TodoChecklistProjection.class))
            .withConfigurationResource("application.properties");

    record ListTodos() implements Input {
    }

    @Inject
    StubQueryUseCaseCaller stubQueryUseCaseCaller;

    @ApplicationScoped
    static class NoAudienceQueryUseCase implements QueryUseCase<TodoId, ListTodos, TodoProjection> {

        @Override
        public Result<TodoId, TodoProjection> execute(final ListTodos input) throws QueryException {
            return Result.of(List.of(), Set.of());
        }

        @Override
        public List<Permission<TodoId>> permissions() {
            return List.of();
        }
    }

    @ApplicationScoped
    static class StubQueryUseCaseCaller {

        private List<String> called = new ArrayList<>();

        public void add(final String call) {
            Objects.requireNonNull(call);
            this.called.add(call);
        }

        public List<String> called() {
            return called;
        }
    }

    @Unremovable
    @Priority(2)
    @Decorator
    static class StubQueryUseCase implements QueryUseCase<TodoId, ListTodos, TodoProjection> {

        @Inject
        @Any
        @Delegate
        QueryUseCase<TodoId, ListTodos, TodoProjection> delegate;

        @Inject
        StubQueryUseCaseCaller stubQueryUseCaseCaller;

        @Override
        public Result<TodoId, TodoProjection> execute(final ListTodos input) throws QueryException {
            Objects.requireNonNull(input);
            stubQueryUseCaseCaller.add("execute");
            return delegate.execute(input);
        }

        @Override
        public List<Permission<TodoId>> permissions() {
            stubQueryUseCaseCaller.add("permissions");
            return delegate.permissions();
        }
    }

    @Inject
    NoAudienceQueryUseCase noAudienceQueryUseCase;

    @Test
    void shouldFailWhenNoAudienceIsDefined() {
        assertThatThrownBy(() -> noAudienceQueryUseCase.execute(new ListTodos()))
                .isExactlyInstanceOf(QueryException.class)
                .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.FORBIDDEN)
                .cause()
                .isExactlyInstanceOf(UnauthorizedException.class);
        assertThat(stubQueryUseCaseCaller.called()).containsExactly("permissions", "permissions", "execute");
    }
}
