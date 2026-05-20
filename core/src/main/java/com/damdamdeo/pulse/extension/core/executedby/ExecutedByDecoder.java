package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Optional;

@FunctionalInterface
public interface ExecutedByDecoder {

    Optional<String> decode(String encoded, OwnedBy ownedBy) throws UnableToDecodeException;
}
