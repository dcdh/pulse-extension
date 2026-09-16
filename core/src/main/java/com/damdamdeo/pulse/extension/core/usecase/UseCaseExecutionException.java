package com.damdamdeo.pulse.extension.core.usecase;

import java.util.Objects;

public final class UseCaseExecutionException extends Exception {

    private final UseCaseExceptionCode useCaseExceptionCode;

    public UseCaseExecutionException(final Throwable cause, final UseCaseExceptionCode useCaseExceptionCode) {
        super(cause);
        this.useCaseExceptionCode = Objects.requireNonNull(useCaseExceptionCode);
    }

    public UseCaseExceptionCode useCaseExceptionCode() {
        return useCaseExceptionCode;
    }
}
