package com.damdamdeo.pulse.extension.core.executedby;

import java.util.Optional;

@FunctionalInterface
public interface ExecutedByDecoder {

    Optional<String> decode(final String encoded);
}
