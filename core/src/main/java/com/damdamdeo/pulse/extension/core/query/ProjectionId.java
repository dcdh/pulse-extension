package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record ProjectionId<A extends AggregateId>(Identifiable id, A linkTo) {

    public ProjectionId {
        Objects.requireNonNull(id);
        Objects.requireNonNull(linkTo);
    }

    public static <A extends AggregateId> ProjectionId<A> from(final A id) {
        Objects.requireNonNull(id);
        return new ProjectionId<>(id, id);
    }

    public static <A extends AggregateId> ProjectionId<A> from(final Identifiable id, final A linkTo) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(linkTo);
        Validate.validState(id.id().startsWith(linkTo.id()));
        return new ProjectionId<>(id, linkTo);
    }
}
