package com.damdamdeo.pulse.extension.writer.deployment.items;

import com.damdamdeo.pulse.extension.core.AggregateId;
import io.quarkus.builder.item.MultiBuildItem;

import java.util.Objects;

public final class AggregateIdBuildItem extends MultiBuildItem {

    private final Class<? extends AggregateId> aggregateIdClazz;

    public AggregateIdBuildItem(final Class<? extends AggregateId> aggregateIdClazz) {
        this.aggregateIdClazz = Objects.requireNonNull(aggregateIdClazz);
    }

    public Class<? extends AggregateId> aggregateIdClazz() {
        return aggregateIdClazz;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AggregateIdBuildItem that = (AggregateIdBuildItem) o;
        return Objects.equals(aggregateIdClazz, that.aggregateIdClazz);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(aggregateIdClazz);
    }
}
