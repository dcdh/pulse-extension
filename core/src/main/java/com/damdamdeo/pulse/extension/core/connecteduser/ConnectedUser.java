package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Objects;

public record ConnectedUser(Username username) implements Identifiable {

    public ConnectedUser {
        Objects.requireNonNull(username);
    }

    @Override
    public String id() {
        return username.username();
    }
}
