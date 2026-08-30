package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;

public class TestUsernameEncoder implements UsernameEncoder {

    public static final UsernameEncoder INSTANCE = new TestUsernameEncoder();

    @Override
    public UsernameEncoded encode(final Username username, final OwnedBy ownedBy) throws UnableToEncodeException {
        Objects.requireNonNull(username);
        Objects.requireNonNull(ownedBy);
        if (new Username("alice@mail.com").equals(username)) {
            return new UsernameEncoded("aliceEncoded");
        } else if (new Username("bob@mail.com").equals(username)) {
            return new UsernameEncoded("bobEncoded");
        } else {
            throw new IllegalStateException("Should not be here");
        }
    }
}
