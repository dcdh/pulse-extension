package com.damdamdeo.pulse.extension.core.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CronTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "0 0 12 * * ?",
            "0 15 10 ? * MON-FRI",
            "0 0/5 14,18 * * ?",
            "0 0 12 1/5 * ? 2026",
            "* * * * *"
    })
    void shouldCreateCronFromValidExpression(final String expression) {
        // Given

        // When
        final Cron cron = new Cron(expression);

        // Then
        assertEquals(expression, cron.cron());
    }

    @Test
    void shouldRejectNullExpression() {
        assertThrows(NullPointerException.class, () -> new Cron(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "0 0",
            "0 0 12 * * ? 2026 EXTRA",
            "0 0 12 * * @",
            "0  0 12 * * ?",
            "0 0 12 * * monday"
    })
    void shouldRejectInvalidExpression(final String expression) {
        // Given

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new Cron(expression));
    }
}
