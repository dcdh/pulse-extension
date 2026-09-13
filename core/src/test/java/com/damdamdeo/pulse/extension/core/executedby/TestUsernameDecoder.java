package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;

public class TestUsernameDecoder implements UsernameDecoder {

    public static final TestUsernameDecoder INSTANCE = new TestUsernameDecoder();

    @Override
    public Username decode(final UsernameEncoded usernameEncoded, final OwnedBy ownedBy) throws UnableToDecodeException {
        Objects.requireNonNull(usernameEncoded);
        Objects.requireNonNull(ownedBy);
        if (new UsernameEncoded("aliceEncoded").equals(usernameEncoded)) {
            return new Username("alice@mail.com");
        } else if (new UsernameEncoded("bobEncoded").equals(usernameEncoded)) {
            return new Username("bob@mail.com");
        } else {
            throw new IllegalStateException("Should not be here");
        }
    }
}
