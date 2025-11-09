package com.damdamdeo.pulse.extension.consumer.deployment.items;

import com.damdamdeo.pulse.extension.core.consumer.ApplicationNaming;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import io.quarkus.builder.item.MultiBuildItem;

import java.util.List;
import java.util.Objects;

public final class TargetBuildItem extends MultiBuildItem {

    private final Target target;
    private final List<ApplicationNaming> sources;

    public TargetBuildItem(final Target target, final List<ApplicationNaming> sources) {
        this.target = Objects.requireNonNull(target);
        this.sources = Objects.requireNonNull(sources);
    }

    public Target target() {
        return target;
    }

    public List<ApplicationNaming> sources() {
        return sources;
    }
}
