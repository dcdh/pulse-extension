package com.damdamdeo.pulse.extension.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TodoIdTest {

    @Test
    void shouldCreateFromFrom() {
        // Given
        final String id = TodoId.USER_1_TODO_1.id();

        // When
        final TodoId from = TodoId.from(id);

        // Then
        assertEquals(TodoId.USER_1_TODO_1, from);
    }
}
