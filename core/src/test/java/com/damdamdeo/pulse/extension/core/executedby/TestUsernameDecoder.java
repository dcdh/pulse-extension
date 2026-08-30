package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public class TestUsernameDecoder implements UsernameDecoder {

    public static final TestUsernameDecoder INSTANCE = new TestUsernameDecoder();

    @Override
    public Username decode(final UsernameEncoded usernameEncoded, final OwnedBy ownedBy) throws UnableToDecodeException {
        return new Username(usernameEncoded.encoded().replace("encoded", ""));
    }
}
