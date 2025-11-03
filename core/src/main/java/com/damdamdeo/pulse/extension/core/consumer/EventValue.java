package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.time.Instant;

public interface EventValue {

    Instant toCreationDate();

    EventType toEventType();

    EncryptedPayload toEncryptedEventPayload();

    OwnedBy toOwnedBy();
}
