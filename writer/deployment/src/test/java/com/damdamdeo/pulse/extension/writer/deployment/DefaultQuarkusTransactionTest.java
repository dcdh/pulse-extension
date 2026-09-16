package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.CommandCallable;
import com.damdamdeo.pulse.extension.core.command.CommandException;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultQuarkusTransaction;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultQuarkusTransactionTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @Inject
    DefaultQuarkusTransaction defaultQuarkusTransaction;

    @Test
    void shouldRequiringNewThrowCommandException() {
        // Given
        final CommandCallable<AggregateRoot<?>> boom = () -> {
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
        final CommandCallable<AggregateRoot<?>> boom = () -> {
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
