package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.usecase.UseCase;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class UseCaseTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .withConfigurationResource("application.properties");

    @Inject
    Instance<CreateTodoUseCase> createTodoUseCaseInstance;

    @Test
    void shouldInjectUseCase() {
        assertThat(createTodoUseCaseInstance.isResolvable()).isTrue();
    }

    static class CreateTodoUseCase implements UseCase<TodoId, CreateTodo, Todo> {

        @Override
        public Todo execute(final CreateTodo command) {
            throw new IllegalStateException("Should not be called");
        }
    }
}
