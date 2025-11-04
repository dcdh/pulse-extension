package com.damdamdeo.pulse.extension.it;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.AggregateRootLoaded;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.DecryptablePayload;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;

public record Call(Target target,
                   AggregateRootType aggregateRootType,
                   AggregateId aggregateId,
                   CurrentVersionInConsumption currentVersionInConsumption,
                   Instant creationDate,
                   EventType eventType,
                   EncryptedPayload encryptedPayload,
                   OwnedBy ownedBy,
                   DecryptablePayload<JsonNode> decryptableEventPayload,
                   AggregateRootLoaded<JsonNode> aggregateRootLoaded) {

    public Call {
        Objects.requireNonNull(target);
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(currentVersionInConsumption);
        Objects.requireNonNull(creationDate);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(encryptedPayload);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(decryptableEventPayload);
        Objects.requireNonNull(aggregateRootLoaded);
    }
}
