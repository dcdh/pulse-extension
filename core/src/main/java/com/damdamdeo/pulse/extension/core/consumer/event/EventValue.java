package com.damdamdeo.pulse.extension.core.consumer.event;

import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.UsernameDecoder;
import com.damdamdeo.pulse.extension.core.executedby.UnableToDecodeException;

import java.time.ZonedDateTime;

public interface EventValue {

    ZonedDateTime toStoredAt();

    EventType toEventType();

    Encrypted<byte[]> toEncryptedEventPayload();

    OwnedBy toOwnedBy();

    ExecutedBy toExecutedBy(UsernameDecoder usernameDecoder) throws UnableToDecodeException;

    BelongsTo toBelongsTo();
}
