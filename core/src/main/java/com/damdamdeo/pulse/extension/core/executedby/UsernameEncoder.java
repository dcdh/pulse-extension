package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

@FunctionalInterface
public interface UsernameEncoder {

    UsernameEncoded encode(Username username, OwnedBy ownedBy) throws UnableToEncodeException;
}
