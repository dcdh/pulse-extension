package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.time.Instant;
import java.util.function.Supplier;

public interface AsyncEventChannelMessageHandler<T> {

    void handleMessage(Target target,
                       AggregateRootType aggregateRootType,
                       AggregateId aggregateId,
                       CurrentVersionInConsumption currentVersionInConsumption,
                       Instant creationDate,
                       EventType eventType,
                       EncryptedPayload encryptedPayload,
                       OwnedBy ownedBy,
                       T decryptedEventPayload,
                       Supplier<AggregateRootLoaded<T>> aggregateRootLoadedSupplier);
}
