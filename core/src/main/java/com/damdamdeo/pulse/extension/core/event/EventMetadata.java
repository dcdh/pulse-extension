package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.Objects;

public record EventMetadata(AggregateRootType aggregateRootType, EventType eventType, AggregateVersion aggregateVersion,
                            StoredAt storedAt, OwnedBy ownedBy, BelongsTo belongsTo, ExecutedBy executedBy) {

    public EventMetadata {
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(aggregateVersion);
        Objects.requireNonNull(storedAt);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(belongsTo);
        Objects.requireNonNull(executedBy);
    }
}
