package com.damdamdeo.pulse.extension.core.traceability;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultExecutedAtProviderTest {

    private final DefaultExecutedAtProvider provider = new DefaultExecutedAtProvider();

    @Test
    void shouldNowCurrentDateTime() {
        // Given
        final ZonedDateTime before = ZonedDateTime.now();

        // When
        final ExecutedAt executedAt = provider.now();

        // Then
        final ZonedDateTime after = ZonedDateTime.now();

        assertThat(executedAt.at()).isBetween(before, after);
    }
}
