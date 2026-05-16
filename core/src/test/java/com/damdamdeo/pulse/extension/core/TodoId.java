package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Objects;

public record TodoId(String user, SequenceNumber sequence) implements AggregateId {

    public static final SequenceNumber SEQUENCE_NUMBER_1 = SequenceNumber.fromNumber(1L);
    public static final SequenceNumber SEQUENCE_NUMBER_2 = SequenceNumber.fromNumber(2L);
    public static final SequenceNumber SEQUENCE_NUMBER_3 = SequenceNumber.fromNumber(3L);
    public static final SequenceNumber SEQUENCE_NUMBER_4 = SequenceNumber.fromNumber(4L);
    public static final SequenceNumber SEQUENCE_NUMBER_5 = SequenceNumber.fromNumber(5L);
    public static final SequenceNumber SEQUENCE_NUMBER_6 = SequenceNumber.fromNumber(6L);
    public static final SequenceNumber SEQUENCE_NUMBER_7 = SequenceNumber.fromNumber(7L);
    public static final SequenceNumber SEQUENCE_NUMBER_8 = SequenceNumber.fromNumber(8L);
    public static final SequenceNumber SEQUENCE_NUMBER_9 = SequenceNumber.fromNumber(9L);
    public static final SequenceNumber SEQUENCE_NUMBER_10 = SequenceNumber.fromNumber(10L);
    public static final SequenceNumber SEQUENCE_NUMBER_11 = SequenceNumber.fromNumber(11L);
    public static final SequenceNumber SEQUENCE_NUMBER_12 = SequenceNumber.fromNumber(12L);
    public static final SequenceNumber SEQUENCE_NUMBER_13 = SequenceNumber.fromNumber(13L);
    public static final SequenceNumber SEQUENCE_NUMBER_14 = SequenceNumber.fromNumber(14L);

    public TodoId {
        Objects.requireNonNull(user);
        Objects.requireNonNull(sequence);
    }

    public static TodoId from(final Identifiable identifiable) {
        String[] split = identifiable.id().split(SEPARATOR);
        return new TodoId(split[0], new SequenceNumber(split[1]));
    }

    public String id() {
        return user + SEPARATOR + sequence.number();
    }
}
