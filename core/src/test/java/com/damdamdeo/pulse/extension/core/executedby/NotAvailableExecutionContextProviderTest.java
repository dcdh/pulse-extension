package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class NotAvailableExecutionContextProviderTest {

    NotAvailableExecutionContextProvider notAvailableExecutedByProvider;

    @BeforeEach
    void setup() {
        notAvailableExecutedByProvider = new NotAvailableExecutionContextProvider();
    }

    @Test
    void shouldReturnNotAvailable() {
        // Given

        // When
        final ExecutionContext executionContext = notAvailableExecutedByProvider.provide();

        // Then
        assertAll(
                () -> assertThat(executionContext.executedBy()).isEqualTo(ExecutedBy.NotAvailable.INSTANCE),
                () -> assertThat(executionContext.roles()).isEmpty()
        );
    }
}
