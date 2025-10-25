package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.time.Instant;

public interface EventRecord {

    String aggregateRootId();

    String aggregateRootType();

    Integer version();

    AggregateId toAggregateId();

    AggregateRootType toAggregateRootType();

    CurrentVersionInConsumption toCurrentVersionInConsumption();

    Instant toCreationDate();

    EventType toEventType();

    EncryptedPayload toEncryptedEventPayload();

    OwnedBy toOwnedBy();
}
