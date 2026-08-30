package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;

import java.util.Objects;

public final class DefaultUsernameHasher implements UsernameHasher {

    private final Hasher hasher;

    public DefaultUsernameHasher(final Hasher hasher) {
        this.hasher = Objects.requireNonNull(hasher);
    }

    @Override
    public UsernameHashed hash(final Username username) {
        Objects.requireNonNull(username);
        return new UsernameHashed(hasher.hash(username.username()));
    }
}
