package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer;

import java.util.Objects;

public record NotifyEvent(String eventName, Object data,
                          String userId) {// nullable → broadcast tenant

    public NotifyEvent {
        Objects.requireNonNull(eventName);
        Objects.requireNonNull(data);
    }

    public Class<?> type() {
        return data.getClass();
    }

    public boolean shouldBroadcastToUnknownClients() {
        return userId == null;
    }
}
