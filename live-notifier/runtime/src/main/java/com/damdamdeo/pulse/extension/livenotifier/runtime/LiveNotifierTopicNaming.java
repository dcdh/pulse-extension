package com.damdamdeo.pulse.extension.livenotifier.runtime;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;

import java.util.Objects;

public record LiveNotifierTopicNaming(FromApplication fromApplication) {

    public LiveNotifierTopicNaming {
        Objects.requireNonNull(fromApplication);
    }

    public String name() {
        final String schema = "%s_%s".formatted(
                fromApplication.functionalDomain().toLowerCase(),
                fromApplication.componentName().toLowerCase());
        return "pulse.live-notification.%s".formatted(schema);
    }
}
