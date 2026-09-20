package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SpecificDomainTest {

    SpecificDomain<TodoId, MarkTodoAsDone> permission;

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @BeforeEach
    void setup() {
        permission = new SpecificDomain<>() {

            @Override
            public boolean allow(final TodoId aggregateId, final MarkTodoAsDone command, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
                return false;
            }

            @Override
            public boolean allow(final MarkTodoAsDone command, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
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
