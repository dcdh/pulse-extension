package com.damdamdeo.pulse.extension.writer.deployment.items;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import io.quarkus.builder.item.MultiBuildItem;

import java.util.Objects;

public final class AggregateRootBuildItem extends MultiBuildItem {

    private final Class<? extends AggregateRoot<?>> aggregateRootClazz;
    private final Class<? extends AggregateId> aggregateIdClazz;

    public AggregateRootBuildItem(final Class<? extends AggregateRoot<?>> aggregateRootClazz, final Class<? extends AggregateId> aggregateIdClazz) {
        this.aggregateRootClazz = Objects.requireNonNull(aggregateRootClazz);
        this.aggregateIdClazz = Objects.requireNonNull(aggregateIdClazz);
    }

    public Class<? extends AggregateRoot<?>> aggregateRootClazz() {
        return aggregateRootClazz;
    }

    public Class<? extends AggregateId> aggregateIdClazz() {
        return aggregateIdClazz;
    }
}
