package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.damdamdeo.pulse.extension.core.traceability.Finder.ROLE_TRACEABILITY_READ;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceabilityFeatureTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ExecutionContextProvider executionContextProvider;

    @InjectMocks
    TraceabilityFeature traceabilityFeature;

    @Test
    void shouldReturnTraceabilityAsName() {
        // Given

        // When
        final String result = traceabilityFeature.name();

        // Then
        assertEquals("TRACEABILITY", result);
    }

    @Test
    void shouldBeEnabledWhenExecutionContextHasTraceabilityReadRole() {
        // Given
        when(executionContextProvider.provide().hasRole(ROLE_TRACEABILITY_READ))
                .thenReturn(true);

        // When
        final boolean result = traceabilityFeature.isEnabled();

        // Then
        assertAll(
                () -> assertTrue(result),
                () -> verify(executionContextProvider.provide()).hasRole(anyString())
        );
    }

    @Test
    void shouldBeDisabledWhenExecutionContextDoesNotHaveTraceabilityReadRole() {
        // Given
        when(executionContextProvider.provide().hasRole(ROLE_TRACEABILITY_READ))
                .thenReturn(false);

        // When
        final boolean result = traceabilityFeature.isEnabled();

        // Then
        assertAll(
                () -> assertFalse(result),
                () -> verify(executionContextProvider.provide()).hasRole(anyString())
        );
    }
}
