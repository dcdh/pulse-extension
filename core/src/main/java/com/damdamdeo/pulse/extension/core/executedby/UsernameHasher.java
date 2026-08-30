package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;

@FunctionalInterface
public interface UsernameHasher {

    UsernameHashed hash(Username username);
}
