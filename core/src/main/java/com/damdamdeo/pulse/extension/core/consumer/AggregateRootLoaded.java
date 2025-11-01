package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.LastAggregateVersion;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;

public record AggregateRootLoaded<T>(AggregateRootType aggregateRootType,
                                     AggregateId aggregateId,
                                     LastAggregateVersion lastAggregateVersion,
                                     EncryptedPayload encryptedAggregateRootPayload,
                                     DecryptablePayload<T> decryptableAggregateRootPayload,
                                     OwnedBy ownedBy,
                                     InRelationWith inRelationWith) {

    public AggregateRootLoaded {
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(lastAggregateVersion);
        Objects.requireNonNull(encryptedAggregateRootPayload);
        Objects.requireNonNull(decryptableAggregateRootPayload);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(inRelationWith);
    }
}
