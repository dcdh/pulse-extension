package com.damdamdeo.pulse.extension.writer.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;
import org.jboss.jandex.DotName;

import java.util.List;
import java.util.Objects;

public final class DiscoveredClassBuildItem extends MultiBuildItem {

    private final DotName source;
    private final List<DotName> fieldTypes;
    private final List<String> fieldNames;

    public DiscoveredClassBuildItem(final DotName source, final List<DotName> fieldTypes, final List<String> fieldNames) {
        this.source = Objects.requireNonNull(source);
        this.fieldTypes = Objects.requireNonNull(fieldTypes);
        this.fieldNames = Objects.requireNonNull(fieldNames);
    }

    public DotName getSource() {
        return source;
    }

    public List<DotName> getFieldTypes() {
        return fieldTypes;
    }

    public List<String> getFieldNames() {
        return fieldNames;
    }
}
