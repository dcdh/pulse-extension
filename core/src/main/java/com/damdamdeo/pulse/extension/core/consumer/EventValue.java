package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByDecoder;

import java.time.Instant;

public interface EventValue {

    Instant toCreationDate();

    EventType toEventType();

    EncryptedPayload toEncryptedEventPayload();

    OwnedBy toOwnedBy();

    ExecutedBy toExecutedBy(ExecutedByDecoder executedByDecoder);

    BelongsTo toBelongsTo();
}
