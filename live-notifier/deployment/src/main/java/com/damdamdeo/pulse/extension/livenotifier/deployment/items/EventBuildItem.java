package com.damdamdeo.pulse.extension.livenotifier.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;

public final class EventBuildItem extends MultiBuildItem {

    private final Class<?> eventClazz;

    public EventBuildItem(final Class<?> eventClazz) {
        this.eventClazz = eventClazz;
    }

    public Class<?> getEventClazz() {
        return eventClazz;
    }
}
