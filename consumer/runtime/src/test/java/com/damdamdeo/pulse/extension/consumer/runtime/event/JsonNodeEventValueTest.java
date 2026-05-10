package com.damdamdeo.pulse.extension.consumer.runtime.event;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

class JsonNodeEventValueTest {

    @Test
    void shouldConvert() {
        // Given

        // When
        final JsonNodeEventValue jsonNodeEventValue = new JsonNodeEventValue(
                ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12), LocalTime.of(13, 46, 40), ZoneOffset.UTC),
                "eventType",
                "payload".getBytes(StandardCharsets.UTF_8),
                "ownedBy",
                "belongsTo",
                "executedBy");

        // Then
        assertThat(jsonNodeEventValue.toStoredAt().toString()).isEqualTo("1970-01-12T13:46:40Z");
    }
}
