package com.damdamdeo.pulse.extension.core.executedby;

@FunctionalInterface
public interface ExecutedByEncoder {

    byte[] encode(String value);
}
