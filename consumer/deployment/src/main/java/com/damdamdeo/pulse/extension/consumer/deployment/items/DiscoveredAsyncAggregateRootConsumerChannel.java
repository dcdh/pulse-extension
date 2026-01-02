package com.damdamdeo.pulse.extension.consumer.deployment.items;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;
import io.quarkus.builder.item.MultiBuildItem;

import java.util.List;
import java.util.Objects;

public final class DiscoveredAsyncAggregateRootConsumerChannel extends MultiBuildItem {

    private final Purpose purpose;
    private final List<FromApplication> fromApplications;

    public DiscoveredAsyncAggregateRootConsumerChannel(final Purpose purpose, final List<FromApplication> fromApplications) {
        this.purpose = Objects.requireNonNull(purpose);
        this.fromApplications = Objects.requireNonNull(fromApplications);
    }

    public Purpose target() {
        return purpose;
    }

    public List<FromApplication> sources() {
        return fromApplications;
    }
}
