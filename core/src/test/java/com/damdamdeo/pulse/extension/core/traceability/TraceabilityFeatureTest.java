package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceabilityFeatureTest {

    private static final String CONFIGURED_ROLE = "custom-traceability-reader";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ExecutionContextProvider executionContextProvider;

    @Mock
    TraceabilityRoleProvider traceabilityRoleProvider;

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
    void shouldBeEnabledWhenExecutionContextHasConfiguredRole() {
        // Given
        when(traceabilityRoleProvider.provide()).thenReturn(CONFIGURED_ROLE);
        when(executionContextProvider.provide().hasRole(CONFIGURED_ROLE))
                .thenReturn(true);

        // When
        final boolean result = traceabilityFeature.isEnabled();

        // Then
        assertAll(
                () -> assertTrue(result),
                () -> verify(executionContextProvider.provide()).hasRole(CONFIGURED_ROLE)
        );
    }

    @Test
    void shouldBeDisabledWhenExecutionContextDoesNotHaveConfiguredRole() {
        // Given
        when(traceabilityRoleProvider.provide()).thenReturn(CONFIGURED_ROLE);
        when(executionContextProvider.provide().hasRole(CONFIGURED_ROLE))
                .thenReturn(false);

        // When
        final boolean result = traceabilityFeature.isEnabled();

        // Then
        assertAll(
                () -> assertFalse(result),
                () -> verify(executionContextProvider.provide()).hasRole(CONFIGURED_ROLE)
        );
    }
}
