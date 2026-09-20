package com.damdamdeo.pulse.extension.writer.deployment.domainusecase;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.permission.Everyone;
import com.damdamdeo.pulse.extension.core.usecase.permission.Permission;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFailOnDirectImplementationOfDomainUseCaseTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            // classes.add(GuardDomainUseCase.class);
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(
                    StubBackendUserVisibilityRolesProvider.class, StubExecutedByResolver.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .withConfigurationResource("application.properties")
            .assertException(throwable -> assertThat(throwable)
                    .isExactlyInstanceOf(DeploymentException.class)
                    .rootCause()
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("Illegal DomainUseCase implementation: com.damdamdeo.pulse.extension.writer.deployment.domainusecase.ShouldFailOnDirectImplementationOfDomainUseCaseTest$CreateTodoDomainUseCase. DomainUseCase may only be implemented by [com.damdamdeo.pulse.extension.core.usecase.AbstractDomainUseCase, com.damdamdeo.pulse.extension.core.usecase.AbstractCreationalDomainUseCase, com.damdamdeo.pulse.extension.core.connecteduser.registration.AbstractRegistrationDomainUseCase, com.damdamdeo.pulse.extension.core.connecteduser.update.AbstractUpdateUserNameUseCase, com.damdamdeo.pulse.extension.core.usecase.GuardDomainUseCase]")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    static class CreateTodoDomainUseCase implements DomainUseCase<TodoId, CreateTodo, Todo> {

        @Override
        public Todo execute(final CreateTodo command) throws UseCaseException {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public List<Permission<TodoId, CreateTodo>> permissions() {
            return List.of(new Everyone<>());
        }
    }
}
