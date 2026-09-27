package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EveryoneTest {

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @Test
    void shouldAllowWhenExecute() throws QueryException {
        // Given

        // When
        final boolean allowed = Everyone.INSTANCE.allow(Set.of(), permissionExecutionContext);

        // Then
        assertTrue(allowed);
    }

    @Test
    void shouldHaveHighestVisibilityPriority() {
        // Given

        // When
        final int priority = Everyone.INSTANCE.priority();

        // Then
        assertEquals(0, priority);
    }
}
