package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer;

import com.damdamdeo.pulse.extension.livenotifier.runtime.Audience;

import java.util.Objects;

public record NotifyEvent(String eventName, Object data, Audience audience) {

    public NotifyEvent {
        Objects.requireNonNull(eventName);
        Objects.requireNonNull(data);
        Objects.requireNonNull(audience);
    }

    public Class<?> type() {
        return data.getClass();
    }

}
