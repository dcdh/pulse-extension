package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifier;

import java.util.Objects;

public record ConnectedUser(Username username) implements ConnectionIdentifier {

    public ConnectedUser {
        Objects.requireNonNull(username);
    }

    @Override
    public String id() {
        return username.username();
    }
}
