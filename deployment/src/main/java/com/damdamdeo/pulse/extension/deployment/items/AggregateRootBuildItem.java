package com.damdamdeo.pulse.extension.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Type;

import java.util.Objects;

public final class AggregateRootBuildItem extends MultiBuildItem {

    private final Class<?> aggregateRootClazz;
    private final Class<?> aggregateIdClazz;

    public AggregateRootBuildItem(final Class<?> aggregateRootClazz, final Class<?> aggregateIdClazz) {
        this.aggregateRootClazz = Objects.requireNonNull(aggregateRootClazz);
        this.aggregateIdClazz = Objects.requireNonNull(aggregateIdClazz);
    }

    public Class<?> aggregateRootClazz() {
        return aggregateRootClazz;
    }

    public Class<?> aggregateIdClazz() {
        return aggregateIdClazz;
    }
}
