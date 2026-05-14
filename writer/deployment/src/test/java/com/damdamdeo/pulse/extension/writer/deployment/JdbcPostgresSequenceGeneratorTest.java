package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.SequenceGenerationException;
import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.damdamdeo.pulse.extension.writer.runtime.JdbcPostgresSequenceGenerator;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public class JdbcPostgresSequenceGeneratorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .overrideConfigKey("quarkus.devservices.enabled", "true")
            .overrideConfigKey("pulse.datasource.init-at-startup", "true")
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresSequenceGenerator jdbcPostgresSequenceGenerator;

    record TodoId() implements AggregateId {

        @Override
        public String id() {
            throw new IllegalStateException("Should not be called");
        }
    }

    record TodoChecklistId() implements AggregateId {

        @Override
        public String id() {
            throw new IllegalStateException("Should not be called");
        }
    }

    @Test
    void shouldCreateASequence() throws SequenceGenerationException {
        // Given
        final Class<? extends AggregateId> given = TodoId.class;

        // When
        final SequenceNumber sequenceNumber = jdbcPostgresSequenceGenerator.nextFor(given);

        // Then
        assertThat(sequenceNumber).isEqualTo(new SequenceNumber(1L));
    }

    @Test
    void shouldGenerateSequenceInOrder() {
        final Class<? extends AggregateId> given = TodoChecklistId.class;

        assertAll(
                () -> assertThat(jdbcPostgresSequenceGenerator.nextFor(given)).isEqualTo(new SequenceNumber(1L)),
                () -> assertThat(jdbcPostgresSequenceGenerator.nextFor(given)).isEqualTo(new SequenceNumber(2L))
        );
    }
}
