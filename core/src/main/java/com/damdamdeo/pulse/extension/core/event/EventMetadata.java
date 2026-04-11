package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.sql.Timestamp;
import java.util.Objects;

public record EventMetadata(String aggregateRootType, String eventType, AggregateVersion aggregateVersion,
                            Timestamp storedAt, OwnedBy ownedBy, BelongsTo belongsTo, ExecutedBy executedBy) {

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
