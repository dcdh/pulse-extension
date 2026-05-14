package com.damdamdeo.pulse.extension.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class AggregateIdGeneratorTest {

    @Mock
    SequenceGenerator sequenceGenerator;

    @InjectMocks
    AggregateIdGenerator aggregateIdGenerator;


    record TodoId(String id) implements AggregateId {

        TodoId {
            Objects.requireNonNull(id);
        }

        @Override
        public String id() {
            return id;
        }
    }

    @Test
    void shouldReturnGeneratedAggregateIdUsingASequence() throws SequenceGenerationException {
        // Given
        doReturn(new SequenceNumber(1L)).when(sequenceGenerator).nextFor(TodoId.class);

        // When
        final TodoId generated = aggregateIdGenerator.generate(TodoId.class,
                sequenceNumber -> new TodoId("T" + AggregateId.SEPARATOR + sequenceNumber.value()));

        // Then
        assertThat(generated).isEqualTo(new TodoId("T-000001"));
    }

    @Test
    void shouldFailWhenIdDoesNotMatchPattern() throws SequenceGenerationException {
        // Given
        doReturn(new SequenceNumber(1L)).when(sequenceGenerator).nextFor(TodoId.class);

        // When && Then
        assertThatThrownBy(() -> aggregateIdGenerator.generate(TodoId.class,
                sequenceNumber -> new TodoId("T" + sequenceNumber.value())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The string T000001 does not match the pattern ^[a-zA-Z]+-[A-Z0-9\\-]+$");
    }
}
