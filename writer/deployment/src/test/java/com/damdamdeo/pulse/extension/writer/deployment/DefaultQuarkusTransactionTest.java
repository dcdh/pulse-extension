package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.CommandCallable;
import com.damdamdeo.pulse.extension.core.command.CommandException;
import com.damdamdeo.pulse.extension.core.command.Handled;
import com.damdamdeo.pulse.extension.writer.deployment.domainusecase.StubBackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.writer.deployment.domainusecase.StubExecutedByResolver;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultQuarkusTransaction;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultQuarkusTransactionTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            // classes.add(GuardDomainUseCase.class);
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(
                    StubBackendUserVisibilityRolesProvider.class, StubExecutedByResolver.class))
            .withConfigurationResource("application.properties");

    @Inject
    DefaultQuarkusTransaction defaultQuarkusTransaction;

    @Test
    void shouldRequiringNewThrowCommandException() {
        // Given
        final CommandCallable<Handled<Todo, TodoId>> boom = () -> {
            throw new CommandException(
                    new IllegalStateException("BOOM"));
        };

        // When && Then
        assertThatThrownBy(() -> defaultQuarkusTransaction.requiringNew(boom))
                .isInstanceOf(CommandException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("BOOM");
    }

    @Test
    void shouldJoiningExistingThrowCommandException() {
        // Given
        final CommandCallable<Handled<Todo, TodoId>> boom = () -> {
            throw new CommandException(
                    new IllegalStateException("BOOM"));
        };

        // When && Then
        assertThatThrownBy(() -> defaultQuarkusTransaction.joiningExisting(boom))
                .isInstanceOf(CommandException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("BOOM");
    }
}
