package com.damdamdeo.pulse.extension.common.runtime.executedby;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class NotAvailableExecutedByProviderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false");

    @Inject
    com.damdamdeo.pulse.extension.core.executedby.ExecutedByProvider executedByProvider;

    @Test
    void shouldReturnUnknown() {
        // Given

        // When
        final ExecutedBy executedBy = executedByProvider.provide();

        // Then
        assertThat(executedBy).isEqualTo(ExecutedBy.NotAvailable.INSTANCE);
    }
}
