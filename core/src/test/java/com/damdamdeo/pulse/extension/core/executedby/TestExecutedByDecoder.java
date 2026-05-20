package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import org.apache.commons.lang3.Validate;

import java.util.Optional;

public class TestExecutedByDecoder implements ExecutedByDecoder {

    public static final TestExecutedByDecoder INSTANCE = new TestExecutedByDecoder();

    @Override
    public Optional<String> decode(final String encoded, final OwnedBy ownedBy) {
        Validate.validState(encoded.startsWith("encoded"));
        return Optional.of(encoded.replace("encoded", ""));
    }
}
