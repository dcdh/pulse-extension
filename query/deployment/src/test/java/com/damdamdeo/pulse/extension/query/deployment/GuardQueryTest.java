package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.query.*;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GuardQueryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubPassphraseProvider.class,
                    TodoProjection.class, TodoChecklistProjection.class))
            .withConfigurationResource("application.properties");

    record ListTodos() implements Input {
    }

    @ApplicationScoped
    static class NoAudienceQuery implements Query<ListTodos, TodoProjection> {

        @Override
        public Result<TodoProjection> execute(final ListTodos input) throws QueryException {
            return Result.of(List.of(), Set.of());
        }

        @Override
        public List<Audience> audiences() {
            return List.of();
        }
    }

    @Inject
    NoAudienceQuery noAudienceQuery;

    @Test
    void shouldFailWhenNoAudienceIsDefined() {
        assertThatThrownBy(() -> noAudienceQuery.execute(new ListTodos()))
                .isExactlyInstanceOf(QueryException.class)
                .cause()
                .isExactlyInstanceOf(DisallowException.class);
    }

}
