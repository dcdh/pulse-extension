package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;

import java.security.Principal;
import java.util.Objects;
import java.util.Optional;

public final class ConnectedClient implements Client {

    private final Username identifier;

    public ConnectedClient(final Principal principal) {
        Objects.requireNonNull(principal);
        this.identifier = new Username(Objects.requireNonNull(principal.getName()));
    }

    @Override
    public Optional<Username> username() {
        return Optional.of(identifier);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ConnectedClient that = (ConnectedClient) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(identifier);
    }
}
