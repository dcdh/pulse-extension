package com.damdamdeo.pulse.extension.common.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;

public final class EligibleTypeForSerializationBuildItem extends MultiBuildItem {

    private final Class<?> clazz;

    public EligibleTypeForSerializationBuildItem(final Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> clazz() {
        return clazz;
    }
}
