package com.damdamdeo.pulse.extension.writer.deployment.items;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import io.quarkus.builder.item.MultiBuildItem;

import java.util.Objects;

public final class IdentifiableBuildItem extends MultiBuildItem {

    private final Class<? extends Identifiable> identifiableClazz;

    public IdentifiableBuildItem(final Class<? extends Identifiable> identifiableClazz) {
        this.identifiableClazz = Objects.requireNonNull(identifiableClazz);
    }

    public Class<? extends Identifiable> identifiableClazz() {
        return identifiableClazz;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdentifiableBuildItem that = (IdentifiableBuildItem) o;
        return Objects.equals(identifiableClazz, that.identifiableClazz);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(identifiableClazz);
    }
}
