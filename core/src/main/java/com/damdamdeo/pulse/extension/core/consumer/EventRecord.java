package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.time.Instant;

public interface EventRecord {

    AggregateRootType toAggregateRootType();

    AggregateId toAggregateId();

    CurrentVersionInConsumption toCurrentVersionInConsumption();

    Instant toCreationDate();

    EventType toEventType();

    EncryptedPayload toEncryptedEventPayload();

    OwnedBy toOwnedBy();

    default boolean match(final EventKey eventKey) {
        return eventKey.toAggregateRootType().equals(toAggregateRootType())
                && eventKey.toAggregateId().equals(toAggregateId())
                && eventKey.toCurrentVersionInConsumption().equals(toCurrentVersionInConsumption());
    }
}
