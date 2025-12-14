package com.damdamdeo.pulse.extension.consumer.deployment.notification;

import io.quarkus.builder.item.MultiBuildItem;

import java.util.Objects;

public final class NotificationChannelBuildItem extends MultiBuildItem {

    private final String functionalDomain;
    private final String componentName;

    public NotificationChannelBuildItem(final String functionalDomain,
                                        final String componentName) {
        this.functionalDomain = Objects.requireNonNull(functionalDomain);
        this.componentName = Objects.requireNonNull(componentName);
    }

    public String functionalDomain() {
        return functionalDomain;
    }

    public String componentName() {
        return componentName;
    }

    public String channel() {
        return "notifier-%s-%s".formatted(functionalDomain.toLowerCase(), componentName.toLowerCase());
    }
}
