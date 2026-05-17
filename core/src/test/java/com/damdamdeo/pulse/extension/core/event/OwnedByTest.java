package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class OwnedByTest {

    record CustomIdentifiable(String id) implements Identifiable {

        CustomIdentifiable {
            Objects.requireNonNull(id);
        }
    }

    @Test
    void shouldGenerateExpectedIdFromAggregateId() {
        // Given
        final Identifiable given = new CustomIdentifiable("0123456789");

        // When
        final OwnedBy from = OwnedBy.from(given);

        // Then
        assertThat(from.id()).isEqualTo("CustomIdentifiable|0123456789");
    }

    @Test
    void shouldGenerateExpectedIdFromIdentifiable() {
        // Given
        final TodoId given = TodoId.USER_1_TODO_1;

        // When
        final OwnedBy from = OwnedBy.from(given);

        // Then
        assertThat(from.id()).isEqualTo("TodoId|U000001-T000001");
    }

    @Test
    void shouldGenerateExpectedIdFromAggregateRoot() {
        // Given
        final Todo given = new Todo(TodoId.USER_1_TODO_1);

        // When
        final OwnedBy himself = OwnedBy.himself(given);

        // Then
        assertThat(himself.id()).isEqualTo("TodoId|U000001-T000001");
    }
}
