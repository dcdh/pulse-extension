package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier;

import java.security.Principal;
import java.util.Objects;

public final class ConnectedClient implements Client {

    private final String identifier;

    public ConnectedClient(final Principal principal) {
        Objects.requireNonNull(principal);
        this.identifier = Objects.requireNonNull(principal.getName());
    }

    @Override
    public String identifier() {
        return identifier;
    }

    @Override
    public boolean isUnknown() {
        return false;
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
