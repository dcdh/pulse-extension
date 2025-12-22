package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier;

import java.util.Objects;
import java.util.UUID;

public final class UnknownClient implements Client {

    private final UUID identifier;

    public UnknownClient(final UUID identifier) {
        this.identifier = Objects.requireNonNull(identifier);
    }

    @Override
    public boolean isUnknown() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UnknownClient that = (UnknownClient) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(identifier);
    }
}
