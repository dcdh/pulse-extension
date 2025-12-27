package com.damdamdeo.pulse.extension.consumer.deployment.items;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import io.quarkus.builder.item.MultiBuildItem;

import java.util.List;
import java.util.Objects;

public final class DiscoveredAsyncEventConsumerChannel extends MultiBuildItem {

    private final Target target;
    private final List<FromApplication> fromApplications;

    public DiscoveredAsyncEventConsumerChannel(final Target target, final List<FromApplication> fromApplications) {
        this.target = Objects.requireNonNull(target);
        this.fromApplications = Objects.requireNonNull(fromApplications);
    }

    public Target target() {
        return target;
    }

    public List<FromApplication> sources() {
        return fromApplications;
    }
}
