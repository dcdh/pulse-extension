package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

@FunctionalInterface
public interface ExecutedByEncoder {

    Encrypted<byte[]> encode(String value, OwnedBy ownedBy) throws UnableToEncodeException;
}
