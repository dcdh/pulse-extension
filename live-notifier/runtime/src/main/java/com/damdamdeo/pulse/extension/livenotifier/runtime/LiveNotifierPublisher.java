package com.damdamdeo.pulse.extension.livenotifier.runtime;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface LiveNotifierPublisher<T> {

    void publish(String eventName, T payload, OwnedBy ownedBy);

    void publish(String eventName, T payload);
}
