package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.nio.charset.StandardCharsets;

public class TestExecutedByEncoder implements ExecutedByEncoder {

    public static final ExecutedByEncoder INSTANCE = new TestExecutedByEncoder();

    @Override
    public byte[] encode(final String value, final OwnedBy ownedBy) throws UnableToEncodeException {
        return ("encoded" + value).getBytes(StandardCharsets.UTF_8);
    }
}
