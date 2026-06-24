package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.UserId;

import java.util.Objects;

public record UserUpdateUsername(UserId id) implements Command<UserId> {

    public UserUpdateUsername {
        Objects.requireNonNull(id);
    }
}
