package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;

import java.util.Optional;

public final class UnknownClient implements Client {

    public UnknownClient() {
    }

    @Override
    public Optional<Username> username() {
        return Optional.empty();
    }
}
