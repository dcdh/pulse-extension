package com.damdamdeo.pulse.extension.core.executedby;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotAvailableExecutedByProviderTest {

    NotAvailableExecutedByProvider notAvailableExecutedByProvider;

    @BeforeEach
    void setup() {
        notAvailableExecutedByProvider = new NotAvailableExecutedByProvider();
    }

    @Test
    void shouldReturnNotAvailable() {
        // Given

        // When
        final ExecutedBy executedBy = notAvailableExecutedByProvider.provide();

        // Then
        assertThat(executedBy).isEqualTo(ExecutedBy.NotAvailable.INSTANCE);
    }
}