package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByFactory;
import com.damdamdeo.pulse.extension.core.executedby.UnableToDecodeException;

import java.time.ZonedDateTime;

public interface EventValue {

    ZonedDateTime toStoredAt();

    EventType toEventType();

    EncryptedPayload toEncryptedEventPayload();

    OwnedBy toOwnedBy();

    ExecutedBy toExecutedBy(ExecutedByFactory executedByFactory) throws UnableToDecodeException;

    BelongsTo toBelongsTo();
}
