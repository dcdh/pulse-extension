package com.damdamdeo.pulse.extension.core.consumer.aggregateroot;

import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface AggregateRootValue {

    EncryptedPayload toEncryptedPayload();

    OwnedBy toOwnedBy();

    BelongsTo toBelongsTo();
}
