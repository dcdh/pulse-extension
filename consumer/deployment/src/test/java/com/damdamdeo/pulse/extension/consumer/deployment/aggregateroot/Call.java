package com.damdamdeo.pulse.extension.consumer.deployment.aggregateroot;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.DecryptablePayload;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public record Call(FromApplication fromApplication,
                   Purpose purpose,
                   AggregateRootType aggregateRootType,
                   AggregateId aggregateId,
                   CurrentVersionInConsumption currentVersionInConsumption,
                   EncryptedPayload encryptedPayload,
                   OwnedBy ownedBy,
                   BelongsTo belongsTo,
                   DecryptablePayload<JsonNode> decryptableEventPayload) {

    public Call {
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(currentVersionInConsumption);
        Objects.requireNonNull(encryptedPayload);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(belongsTo);
        Objects.requireNonNull(decryptableEventPayload);
    }
}
