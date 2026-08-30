package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public class TestUsernameEncoder implements UsernameEncoder {

    public static final UsernameEncoder INSTANCE = new TestUsernameEncoder();

    @Override
    public UsernameEncoded encode(final Username username, final OwnedBy ownedBy) throws UnableToEncodeException {
        return new UsernameEncoded("encoded");
    }
}
