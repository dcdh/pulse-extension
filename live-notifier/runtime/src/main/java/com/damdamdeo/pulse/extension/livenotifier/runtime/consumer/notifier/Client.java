package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;

import java.util.Optional;

public interface Client {

    Optional<Username> username();
}
