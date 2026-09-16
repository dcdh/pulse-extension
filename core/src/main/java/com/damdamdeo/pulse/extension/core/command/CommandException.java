package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.MissingAggregateException;

public final class CommandException extends Exception {

    private final CommandExceptionCode commandExceptionCode;

    public CommandException(final BusinessException businessException) {
        super(businessException);
        this.commandExceptionCode = CommandExceptionCode.BUSINESS_FAILURE;
    }

    public CommandException(final MissingAggregateException missingAggregateException) {
        super(missingAggregateException);
        this.commandExceptionCode = CommandExceptionCode.BUSINESS_FAILURE;
    }

    public CommandException(final Throwable cause) {
        super(cause);
        this.commandExceptionCode = CommandExceptionCode.INFRASTRUCTURE_FAILURE;
    }

    public CommandExceptionCode commandExceptionCode() {
        return commandExceptionCode;
    }
}
