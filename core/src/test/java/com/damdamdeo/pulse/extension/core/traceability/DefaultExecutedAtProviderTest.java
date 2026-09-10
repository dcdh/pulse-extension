package com.damdamdeo.pulse.extension.core.traceability;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultExecutedAtProviderTest {

    private final DefaultExecutedAtProvider provider = new DefaultExecutedAtProvider();

    @Test
    void shouldNowCurrentDateTime() {
        // Given
        final Instant before = Instant.now();

        // When
        final ExecutedAt executedAt = provider.now();

        // Then
        final Instant after = Instant.now();

        assertThat(executedAt.at()).isBetween(before, after);
    }
}
