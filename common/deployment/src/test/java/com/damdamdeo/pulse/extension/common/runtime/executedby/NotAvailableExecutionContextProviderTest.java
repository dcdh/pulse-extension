package com.damdamdeo.pulse.extension.common.runtime.executedby;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class NotAvailableExecutionContextProviderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false");

    @Inject
    ExecutionContextProvider executionContextProvider;

    @Test
    void shouldReturnUnknown() {
        // Given

        // When
        final ExecutionContext executionContext = executionContextProvider.provide();

        // Then
        assertAll(
                () -> assertThat(executionContext.executedBy()).isEqualTo(ExecutedBy.NotAvailable.INSTANCE),
                () -> assertThat(executionContext.roles()).isEmpty()
        );
    }
}
