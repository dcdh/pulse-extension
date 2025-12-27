package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.DecryptablePayload;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.time.Instant;
import java.util.function.Supplier;

public interface AsyncEventChannelMessageHandler<T> {

    void handleMessage(FromApplication fromApplication,
                       Target target,
                       AggregateRootType aggregateRootType,
                       AggregateId aggregateId,
                       CurrentVersionInConsumption currentVersionInConsumption,
                       Instant creationDate,
                       EventType eventType,
                       EncryptedPayload encryptedPayload,
                       OwnedBy ownedBy,
                       BelongsTo belongsTo,
                       ExecutedBy executedBy,
                       DecryptablePayload<T> decryptableEventPayload,
                       Supplier<AggregateRootLoaded<T>> aggregateRootLoadedSupplier);
}
