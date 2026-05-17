package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Objects;

public record UserId(SequenceNumber sequence) implements AggregateId {

    public static final UserId USER_1 = new UserId(SequenceNumber.fromNumber(1L));
    public static final UserId USER_2 = new UserId(SequenceNumber.fromNumber(2L));

    public UserId {
        Objects.requireNonNull(sequence);
    }

    @Override
    public String id() {
        return "U" + sequence.number();
    }

    public static UserId from(final Identifiable identifiable) {
        final String[] split = identifiable.id().split(SEPARATOR);
        return new UserId(new SequenceNumber(split[1].substring(1)));
    }
}
