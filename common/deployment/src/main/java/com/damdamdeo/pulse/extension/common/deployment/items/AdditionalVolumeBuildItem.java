package com.damdamdeo.pulse.extension.common.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;

import java.util.Objects;

public final class AdditionalVolumeBuildItem extends MultiBuildItem {

    private final ComposeServiceBuildItem.ServiceName serviceName;
    private final ComposeServiceBuildItem.Volume volume;

    public AdditionalVolumeBuildItem(final ComposeServiceBuildItem.ServiceName serviceName,
                                     final ComposeServiceBuildItem.Volume volume) {
        this.serviceName = Objects.requireNonNull(serviceName);
        this.volume = Objects.requireNonNull(volume);
    }

    public ComposeServiceBuildItem.ServiceName getServiceName() {
        return serviceName;
    }

    public ComposeServiceBuildItem.Volume getVolume() {
        return volume;
    }
}
