package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;

public class TestUsernameHasher implements UsernameHasher {

    public static final UsernameHasher INSTANCE = new TestUsernameHasher();

    @Override
    public UsernameHashed hash(final Username username) {
        return new UsernameHashed("hashed-username");
    }
}
