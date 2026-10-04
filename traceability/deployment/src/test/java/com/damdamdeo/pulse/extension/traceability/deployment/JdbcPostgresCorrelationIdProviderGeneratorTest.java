package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.traceability.CorrelationId;
import com.damdamdeo.pulse.extension.core.traceability.CorrelationIdGeneratorException;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresCorrelationIdGenerator;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcPostgresCorrelationIdProviderGeneratorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresCorrelationIdGenerator jdbcPostgresCorrelationIdGenerator;

    @Test
    void shouldGenerateCorrelationIds() throws CorrelationIdGeneratorException {
        // Given

        // When
        final List<CorrelationId> correlationIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            correlationIds.add(jdbcPostgresCorrelationIdGenerator.generate());
        }

        // Then
        assertThat(correlationIds).containsExactly(new CorrelationId(1L),
                new CorrelationId(2L),
                new CorrelationId(3L),
                new CorrelationId(4L),
                new CorrelationId(5L),
                new CorrelationId(6L),
                new CorrelationId(7L),
                new CorrelationId(8L),
                new CorrelationId(9L),
                new CorrelationId(10L));
    }
}
