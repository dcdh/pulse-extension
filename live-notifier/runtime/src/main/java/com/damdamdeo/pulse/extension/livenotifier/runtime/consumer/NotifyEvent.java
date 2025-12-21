package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer;

import java.util.Objects;

public record NotifyEvent(String eventName, Object data) {

    public NotifyEvent {
        Objects.requireNonNull(eventName);
        Objects.requireNonNull(data);
    }

    public Class<?> type() {
        return data.getClass();
    }
}
