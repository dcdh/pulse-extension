package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record UserId(SequenceNumber sequence) implements AggregateId {

    public static final UserId USER_1 = new UserId(SequenceNumber.fromNumber(1L));
    public static final UserId USER_2 = new UserId(SequenceNumber.fromNumber(2L));
    public static final UserId USER_3 = new UserId(SequenceNumber.fromNumber(3L));
    public static final UserId USER_4 = new UserId(SequenceNumber.fromNumber(4L));
    public static final UserId USER_5 = new UserId(SequenceNumber.fromNumber(5L));
    public static final UserId USER_6 = new UserId(SequenceNumber.fromNumber(6L));
    public static final UserId USER_7 = new UserId(SequenceNumber.fromNumber(7L));

    public UserId {
        Objects.requireNonNull(sequence);
    }

    @Override
    public String id() {
        return "U" + sequence.number();
    }

    public static UserId from(final Identifiable identifiable) {
        Validate.validState(identifiable.id().startsWith("U"), "invalid id " + identifiable.id());
        return new UserId(new SequenceNumber(identifiable.id().substring(1)));
    }
}
