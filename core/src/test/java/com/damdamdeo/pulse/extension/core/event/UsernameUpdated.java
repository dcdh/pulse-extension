package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;

import java.util.Objects;

public record UsernameUpdated(Username username) implements Event<UserId> {

    public UsernameUpdated {
        Objects.requireNonNull(username);
    }
}
