package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;

public record NotifyEvent(String eventName, Object data,
                          OwnedBy ownedBy) {// nullable → broadcast all

    public NotifyEvent {
        Objects.requireNonNull(eventName);
        Objects.requireNonNull(data);
    }

    public Class<?> type() {
        return data.getClass();
    }

    public boolean shouldBroadcastToUnknownClients() {
        return ownedBy == null;
    }
}
