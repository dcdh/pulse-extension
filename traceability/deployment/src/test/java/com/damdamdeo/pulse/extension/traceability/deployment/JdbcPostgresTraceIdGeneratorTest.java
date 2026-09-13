package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.traceability.TraceId;
import com.damdamdeo.pulse.extension.core.traceability.TraceIdGeneratorException;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresTraceIdGenerator;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcPostgresTraceIdGeneratorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresTraceIdGenerator jdbcPostgresTraceIdGenerator;

    @Test
    void shouldGenerateTraceIds() throws TraceIdGeneratorException {
        // Given

        // When
        final List<TraceId> traceIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            traceIds.add(jdbcPostgresTraceIdGenerator.generate());
        }

        // Then
        assertThat(traceIds).containsExactly(new TraceId(1L),
                new TraceId(2L),
                new TraceId(3L),
                new TraceId(4L),
                new TraceId(5L),
                new TraceId(6L),
                new TraceId(7L),
                new TraceId(8L),
                new TraceId(9L),
                new TraceId(10L));
    }
}
