package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

// TODO use a cache mechanism - complexe must handle banned key
@FunctionalInterface
public interface UsernameDecoder {

    Username decode(UsernameEncoded usernameEncoded, OwnedBy ownedBy) throws UnableToDecodeException;
}
