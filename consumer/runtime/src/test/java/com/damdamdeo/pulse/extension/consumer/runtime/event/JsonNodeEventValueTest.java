package com.damdamdeo.pulse.extension.consumer.runtime.event;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JsonNodeEventValueTest {

    @Test
    void shouldConvert() {
        // Given

        // When
        final JsonNodeEventValue jsonNodeEventValue = new JsonNodeEventValue(
                1767383449340168L,
                "eventType",
                "payload".getBytes(StandardCharsets.UTF_8),
                "ownedBy",
                "belongsTo",
                "executedBy");

        // Then
        assertThat(jsonNodeEventValue.toCreationDate().toString()).isEqualTo("2026-01-02T19:50:49.340Z");
    }
}