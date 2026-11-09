package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.UserId;

import java.util.Objects;

public record UserUpdateUsername(UserId aggregateId) implements Command<UserId> {

    public UserUpdateUsername {
        Objects.requireNonNull(aggregateId);
    }

    @Override
    public CommandId<UserId> id() {
        return CommandId.from(aggregateId);
    }
}
