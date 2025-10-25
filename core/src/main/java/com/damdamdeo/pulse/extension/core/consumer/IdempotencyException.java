package com.damdamdeo.pulse.extension.core.consumer;

public class IdempotencyException extends RuntimeException {

    public IdempotencyException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
