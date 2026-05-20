package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

@FunctionalInterface
public interface ExecutedByEncoder {

    byte[] encode(String value, OwnedBy ownedBy) throws UnableToEncodeException;
}
