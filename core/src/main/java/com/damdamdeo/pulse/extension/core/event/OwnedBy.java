package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Objects;

public record OwnedBy(String id) {

    public OwnedBy {
        Objects.requireNonNull(id);
    }

    public static OwnedBy from(final AggregateId ownedBy) {
        Objects.requireNonNull(ownedBy);
        return new OwnedBy(ownedBy.id());
    }
}
