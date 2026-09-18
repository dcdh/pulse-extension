package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.query.audience.Audience;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
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
