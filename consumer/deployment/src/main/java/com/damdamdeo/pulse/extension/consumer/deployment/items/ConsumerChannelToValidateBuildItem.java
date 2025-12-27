package com.damdamdeo.pulse.extension.consumer.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;

import java.util.Objects;

public final class ConsumerChannelToValidateBuildItem extends MultiBuildItem {

    private final Class<?> clazz;

    public ConsumerChannelToValidateBuildItem(final Class<?> clazz) {
        this.clazz = Objects.requireNonNull(clazz);
    }

    public Class<?> clazz() {
        return clazz;
    }
}
