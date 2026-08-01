package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface AggregateRootValue {

    Encrypted<byte[]> toEncryptedPayload();

    OwnedBy toOwnedBy();

    BelongsTo toBelongsTo();
}
