package com.damdamdeo.pulse.extension.common.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public final class PostgresSqlScriptBuildItem extends MultiBuildItem {

    private final String name;
    private final String content;

    public PostgresSqlScriptBuildItem(final String name, final String content) {
        this.name = Objects.requireNonNull(name);
        Validate.validState(name.matches("[a-zA-Z_0-9]+\\.sql"));
        this.content = Objects.requireNonNull(content);
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }
}
