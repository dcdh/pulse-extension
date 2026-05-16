package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record TodoChecklistId(TodoId todoId, SequenceNumber sequence) implements AggregateId {

    public static final SequenceNumber SEQUENCE_NUMBER_1 = SequenceNumber.fromNumber(1L);
    public static final SequenceNumber SEQUENCE_NUMBER_2 = SequenceNumber.fromNumber(2L);
    public static final SequenceNumber SEQUENCE_NUMBER_3 = SequenceNumber.fromNumber(3L);
    public static final SequenceNumber SEQUENCE_NUMBER_4 = SequenceNumber.fromNumber(4L);

    public TodoChecklistId {
        Objects.requireNonNull(todoId);
        Objects.requireNonNull(sequence);
    }

    @Override
    public String id() {
        return todoId.id() + SEPARATOR + sequence.number();
    }
}
