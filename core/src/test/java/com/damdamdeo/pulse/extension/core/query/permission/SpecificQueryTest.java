package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SpecificQueryTest {

    SpecificQuery<TodoId> permission;

    @BeforeEach
    void setup() {
        permission = new SpecificQuery<TodoId>() {

            @Override
            public boolean allow(final Set<TodoId> aggregateIds, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
                return false;
            }
        };
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given

        // When
        final int priority = permission.priority();

        // Then
        assertEquals(7, priority);
    }
}
