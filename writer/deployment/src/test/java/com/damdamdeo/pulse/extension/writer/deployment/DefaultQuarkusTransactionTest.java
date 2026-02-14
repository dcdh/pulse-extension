package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.command.BusinessCallable;
import com.damdamdeo.pulse.extension.writer.runtime.DefaultQuarkusTransaction;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultQuarkusTransactionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .withConfigurationResource("application.properties");

    @Inject
    DefaultQuarkusTransaction defaultQuarkusTransaction;

    @Test
    void shouldRequiringNewThrowBusinessException() {
        // Given
        BusinessCallable<AggregateRoot<?>> boom = () -> {
            throw new BusinessException(
                    new IllegalStateException("BOOM"));
        };

        // When && Then
        assertThatThrownBy(() -> defaultQuarkusTransaction.requiringNew(boom))
                .isInstanceOf(BusinessException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("BOOM");
    }

    @Test
    void shouldJoiningExistingThrowBusinessException() {
        // Given
        BusinessCallable<AggregateRoot<?>> boom = () -> {
            throw new BusinessException(
                    new IllegalStateException("BOOM"));
        };

        // When && Then
        assertThatThrownBy(() -> defaultQuarkusTransaction.joiningExisting(boom))
                .isInstanceOf(BusinessException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("BOOM");
    }
}
