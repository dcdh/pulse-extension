package com.damdamdeo.pulse.extension.build.report.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.content.Content;
import io.quarkus.builder.item.MultiBuildItem;

import java.util.Objects;

public final class ContentBuildItem extends MultiBuildItem {

    private final Content content;

    public ContentBuildItem(final Content content) {
        this.content = Objects.requireNonNull(content);
    }

    public Content content() {
        return content;
    }
}
