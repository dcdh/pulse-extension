package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.query.audience.Audience;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppender;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardQueryUseCaseTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubPassphraseProvider.class,
                    TodoProjection.class, TodoChecklistProjection.class))
            .withConfigurationResource("application.properties");

    record ListTodos() implements Input {
    }

    // FKC To remove
    @Unremovable
    @Decorator
    @Priority(0)
    static class GuardQueryUseCaseTest_NoAudienceQueryUseCaseGuardQueryGenerated extends GuardQueryUseCase<GuardQueryUseCaseTest.ListTodos, TodoProjection> {
        public GuardQueryUseCaseTest_NoAudienceQueryUseCaseGuardQueryGenerated(ExecutionContextProvider var1, BackendUserVisibilityRolesProvider var2, ExecutedByResolver var3, @Any @Delegate QueryUseCase<GuardQueryUseCaseTest.ListTodos, TodoProjection> var4, TraceAppender var5) {
            super(var1, var2, var3, var4, var5);
        }
    }

    @ApplicationScoped
    static class NoAudienceQueryUseCase implements QueryUseCase<ListTodos, TodoProjection> {

        @Override
        public Result<TodoProjection> execute(final ListTodos input) throws QueryException {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public List<Audience> audiences() {
            return List.of();
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
    }
}
