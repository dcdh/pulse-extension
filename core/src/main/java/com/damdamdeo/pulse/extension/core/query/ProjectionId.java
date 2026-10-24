package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record ProjectionId<A extends AggregateId>(String id, A linkTo) {

    public ProjectionId {
        Objects.requireNonNull(id);
    }

    public static <A extends AggregateId> ProjectionId<A> from(final A id) {
        Objects.requireNonNull(id);
        return new ProjectionId<>(id.id(), id);
    }

    public static <A extends AggregateId> ProjectionId<A> from(final String id, final A linkTo) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(linkTo);
        Validate.validState(id.startsWith(linkTo.id()));
        return new ProjectionId<>(id, linkTo);
    }
}
