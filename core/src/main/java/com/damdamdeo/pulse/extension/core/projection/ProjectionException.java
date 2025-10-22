package com.damdamdeo.pulse.extension.core.projection;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;
import java.util.Optional;

public final class ProjectionException extends RuntimeException {

    private final OwnedBy ownedBy;
    private final AggregateId aggregateId;

    public ProjectionException(final OwnedBy ownedBy, final AggregateId aggregateId, final Throwable cause) {
        super(cause);
        this.ownedBy = Objects.requireNonNull(ownedBy);
        this.aggregateId = Objects.requireNonNull(aggregateId);
    }

    public ProjectionException(final OwnedBy ownedBy, final Throwable cause) {
        super(cause);
        this.ownedBy = Objects.requireNonNull(ownedBy);
        this.aggregateId = null;
    }

    public OwnedBy ownedBy() {
        return ownedBy;
    }

    public Optional<AggregateId> aggregateId() {
        return Optional.ofNullable(aggregateId);
    }
}
