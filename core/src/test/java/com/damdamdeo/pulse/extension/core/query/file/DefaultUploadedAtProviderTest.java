package com.damdamdeo.pulse.extension.core.query.file;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultUploadedAtProviderTest {

    private final DefaultUploadedAtProvider provider = new DefaultUploadedAtProvider();

    @Test
    void shouldNowReturnCurrentDateTime() {
        // Given
        final Instant before = Instant.now();

        // When
        final UploadedAt uploadedAt = provider.now();

        // Then
        final Instant after = Instant.now();

        assertThat(uploadedAt.at()).isBetween(before, after);
    }
}
