package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.usecase.GenericUseCase;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericUseCaseTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .withConfigurationResource("application.properties");

    @Inject
    Instance<CreateTodoGenericUseCase> createTodoGenericUseCaseInstance;

    @Test
    void shouldInjectUseCase() {
        assertThat(createTodoGenericUseCaseInstance.isResolvable()).isTrue();
    }

    static class CreateTodoGenericUseCase implements GenericUseCase<CreateTodo, Todo> {

        @Override
        public Todo execute(final CreateTodo command) throws BusinessException {
            throw new IllegalStateException("Should not be called");
        }
    }
}

