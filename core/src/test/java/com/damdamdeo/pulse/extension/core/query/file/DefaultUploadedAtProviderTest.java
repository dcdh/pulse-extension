package com.damdamdeo.pulse.extension.core.query.file;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultUploadedAtProviderTest {

    private final DefaultUploadedAtProvider provider = new DefaultUploadedAtProvider();

    @Test
    void shouldProvideCurrentDateTime() {
        // Given
        final ZonedDateTime before = ZonedDateTime.now();

        // When
        final UploadedAt uploadedAt = provider.provide();

        // Then
        final ZonedDateTime after = ZonedDateTime.now();

        assertThat(uploadedAt.at()).isBetween(before, after);
    }
}
