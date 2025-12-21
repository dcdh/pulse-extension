package com.damdamdeo.pulse.extension.livenotifier.runtime;

public interface LiveNotifierPublisher<T> {

    void publish(String eventName, T payload);
}
