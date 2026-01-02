package com.damdamdeo.pulse.extension.consumer.deployment.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.event.AggregateRootLoaded;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;

public record Call(FromApplication fromApplication,
                   Purpose purpose,
                   AggregateRootType aggregateRootType,
                   AggregateId aggregateId,
                   CurrentVersionInConsumption currentVersionInConsumption,
                   Instant creationDate,
                   EventType eventType,
                   EncryptedPayload encryptedPayload,
                   OwnedBy ownedBy,
                   BelongsTo belongsTo,
                   ExecutedBy executedBy,
                   DecryptablePayload<JsonNode> decryptableEventPayload,
                   AggregateRootLoaded<JsonNode> aggregateRootLoaded) {

    public Call {
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(currentVersionInConsumption);
        Objects.requireNonNull(creationDate);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(encryptedPayload);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(belongsTo);
        Objects.requireNonNull(executedBy);
        Objects.requireNonNull(decryptableEventPayload);
        Objects.requireNonNull(aggregateRootLoaded);
    }
}
