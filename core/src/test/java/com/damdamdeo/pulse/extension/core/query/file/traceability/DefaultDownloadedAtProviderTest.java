package com.damdamdeo.pulse.extension.core.query.file.traceability;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDownloadedAtProviderTest {

    private final DefaultDownloadedAtProvider provider = new DefaultDownloadedAtProvider();

    @Test
    void shouldNowReturnCurrentDateTime() {
        // Given
        final ZonedDateTime before = ZonedDateTime.now();

        // When
        final DownloadedAt downloadedAt = provider.now();

        // Then
        final ZonedDateTime after = ZonedDateTime.now();

        assertThat(downloadedAt.at()).isBetween(before, after);
    }
}
