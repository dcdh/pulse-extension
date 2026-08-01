package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.DecryptablePayload;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface AsyncAggregateRootChannelMessageHandler<T> {

    void handleMessage(FromApplication fromApplication,
                       Purpose purpose,
                       AggregateRootType aggregateRootType,
                       AggregateId aggregateId,
                       CurrentVersionInConsumption currentVersionInConsumption,
                       Encrypted encrypted,
                       OwnedBy ownedBy,
                       BelongsTo belongsTo,
                       DecryptablePayload<T> decryptablePayload);
}
