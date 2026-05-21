package com.damdamdeo.pulse.extension.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class AggregateIdGeneratorTest {

    @Mock
    SequenceGenerator sequenceGenerator;

    @InjectMocks
    AggregateIdGenerator aggregateIdGenerator;

    @Test
    void shouldReturnGeneratedAggregateIdUsingASequence() throws SequenceGenerationException {
        // Given
        doReturn(SequenceNumber.fromNumber(1L)).when(sequenceGenerator).nextFor(TodoId.class);

        // When
        final TodoId generated = aggregateIdGenerator.generate(TodoId.class,
                sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber));

        // Then
        assertThat(generated).isEqualTo(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));
    }

    record CustomFailingTodoId() implements AggregateId {

        @Override
        public String id() {
            return "T*000001";
        }
    }

    @Test
    void shouldFailWhenIdDoesNotMatchPattern() throws SequenceGenerationException {
        // Given
        doReturn(SequenceNumber.fromNumber(1L)).when(sequenceGenerator).nextFor(CustomFailingTodoId.class);

        // When && Then
        assertThatThrownBy(() -> aggregateIdGenerator.generate(CustomFailingTodoId.class,
                sequenceNumber -> new CustomFailingTodoId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The string T*000001 does not match the pattern ^[a-zA-Z]+-?[A-Z0-9\\-]+$");
    }

    @Test
    void shouldReturnGeneratedAggregateIdUsingASequenceFor() throws SequenceGenerationException {
        // Given
        doReturn(SequenceNumber.fromNumber(1L)).when(sequenceGenerator).nextFor(new For<>(TodoChecklistId.class,
                TodoChecklist.BELONGS_TO_USER_1_TODO_1));

        // When
        final TodoChecklistId generated = aggregateIdGenerator.generate(new For<>(TodoChecklistId.class, TodoChecklist.BELONGS_TO_USER_1_TODO_1),
                sequenceNumber -> new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_10), sequenceNumber));

        // Then
        assertThat(generated).isEqualTo(new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_10), TodoId.SEQUENCE_NUMBER_1));
    }

}
