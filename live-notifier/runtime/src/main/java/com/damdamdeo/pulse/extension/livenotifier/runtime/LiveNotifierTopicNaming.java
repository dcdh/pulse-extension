package com.damdamdeo.pulse.extension.livenotifier.runtime;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;

import java.util.Objects;
import java.util.stream.Collectors;

public record LiveNotifierTopicNaming(FromApplication fromApplication) {

    public LiveNotifierTopicNaming {
        Objects.requireNonNull(fromApplication);
    }

    public String name() {
        return "pulse.live-notification.%s".formatted(fromApplication.applicationNaming()
                .split().stream().map(String::toLowerCase)
                .collect(Collectors.joining("-")));
    }
}
