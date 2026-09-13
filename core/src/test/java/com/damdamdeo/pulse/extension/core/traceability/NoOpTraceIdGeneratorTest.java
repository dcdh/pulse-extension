package com.damdamdeo.pulse.extension.core.traceability;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class NoOpTraceIdGeneratorTest {

    NoOpTraceIdGenerator noOpTraceIdGenerator = new NoOpTraceIdGenerator();

    @Test
    void shouldReturnNotAvailableValue() throws TraceIdGeneratorException {
        // Given

        // When
        final TraceId generated = noOpTraceIdGenerator.generate();

        // Then
        Assertions.assertAll(
                () -> assertThat(generated).isEqualTo(TraceId.NOT_AVAILABLE),
                () -> assertThat(generated.id()).isEqualTo(0L)
        );
    }
}
