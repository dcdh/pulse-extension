package com.damdamdeo.pulse.extension.livenotifier.runtime;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface LiveNotifierPublisher<T> {

    void publish(String eventName, T payload, OwnedBy ownedBy) throws PublicationException;

    void publish(String eventName, T payload) throws PublicationException;
}
