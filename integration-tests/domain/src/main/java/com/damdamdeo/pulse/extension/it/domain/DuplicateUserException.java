package com.damdamdeo.pulse.extension.it.domain;

import com.damdamdeo.pulse.extension.core.DuplicateAggregateException;
import com.damdamdeo.pulse.extension.core.UserId;

import java.util.Objects;

public class DuplicateUserException extends DuplicateAggregateException {

    private final UserId userId;

    public DuplicateUserException(final UserId userId) {
        this.userId = Objects.requireNonNull(userId);
    }
}
