package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record CommandId<A extends AggregateId>(Identifiable id, A linkTo) {

    public CommandId {
        Objects.requireNonNull(id);
        Objects.requireNonNull(linkTo);
    }

    public static <A extends AggregateId> CommandId<A> from(final A id) {
        Objects.requireNonNull(id);
        return new CommandId<>(id, id);
    }

    public static <A extends AggregateId> CommandId<A> from(final Identifiable id, final A linkTo) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(linkTo);
        Validate.validState(id.id().startsWith(linkTo.id()));
        return new CommandId<>(id, linkTo);
    }
}
