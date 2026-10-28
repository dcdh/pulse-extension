package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.traceability.TraceRecorderRepository;
import com.damdamdeo.pulse.extension.core.traceability.TraceabilityDetails;
import com.damdamdeo.pulse.extension.traceability.runtime.ArcTraceRecorderRepositoryProvider;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ArcInvolvedWithFullDetailsTraceRecorderRepositoryProviderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
            .withConfigurationResource("application.properties");

    @Inject
    ArcTraceRecorderRepositoryProvider arcTraceRecorderRepositoryProvider;

    @Test
    void shouldProvideInvolvedTraceRecorderRepositoryOnInvolvedOnly() {
        // Given

        // When
        final TraceRecorderRepository provide = arcTraceRecorderRepositoryProvider.provide(TraceabilityDetails.INVOLVED_ONLY);

        // Then
        assertThat(provide.getClass().getSimpleName()).isEqualTo("JdbcPostgresInvolvedTraceRecorderRepository_ClientProxy");
    }

    @Test
    void shouldProvideInvolvedWithFullDetailsTraceRecorderOnRepositoryDetailsWhenAvailableOtherwiseInvolvedOnly() {
        // Given

        // When
        final TraceRecorderRepository provide = arcTraceRecorderRepositoryProvider.provide(
                TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY);

        // Then
        assertThat(provide.getClass().getSimpleName()).isEqualTo("JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository_ClientProxy");
    }
}
