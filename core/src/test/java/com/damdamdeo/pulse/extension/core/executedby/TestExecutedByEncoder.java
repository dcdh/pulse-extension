package com.damdamdeo.pulse.extension.core.executedby;

import java.nio.charset.StandardCharsets;

public class TestExecutedByEncoder implements ExecutedByEncoder {

    public static final ExecutedByEncoder INSTANCE = new TestExecutedByEncoder();

    @Override
    public byte[] encode(String value) {
        return ("encoded" + value).getBytes(StandardCharsets.UTF_8);
    }
}
