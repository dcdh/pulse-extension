package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.UnauthorizedException;

import java.util.Objects;

public final class UseCaseException extends Exception {

    private final UseCaseExceptionCode useCaseExceptionCode;

    public UseCaseException(final UnauthorizedException exception) {
        this(exception, UseCaseExceptionCode.FORBIDDEN);
    }

    public UseCaseException(final Throwable cause, final UseCaseExceptionCode useCaseExceptionCode) {
        super(cause);
        this.useCaseExceptionCode = Objects.requireNonNull(useCaseExceptionCode);
    }

    public UseCaseExceptionCode useCaseExceptionCode() {
        return useCaseExceptionCode;
    }
}
