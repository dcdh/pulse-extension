package com.damdamdeo.pulse.extension.consumer.runtime.event;

import com.damdamdeo.pulse.extension.core.consumer.event.AsyncEventChannelMessageHandler;
import com.damdamdeo.pulse.extension.core.consumer.event.AsyncEventChannelMessageHandlerProvider;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Singleton
@Unremovable
@DefaultBean
public final class DefaultAsyncEventChannelMessageHandlerProvider implements AsyncEventChannelMessageHandlerProvider<JsonNode> {

    private final Instance<AsyncEventChannelMessageHandler<JsonNode>> eventChannelMessageHandlers;

    private final Map<Purpose, List<AsyncEventChannelMessageHandler<JsonNode>>> cache;

    public DefaultAsyncEventChannelMessageHandlerProvider(@Any final Instance<AsyncEventChannelMessageHandler<JsonNode>> eventChannelMessageHandlers) {
        this.eventChannelMessageHandlers = Objects.requireNonNull(eventChannelMessageHandlers);
        this.cache = new HashMap<>();
    }

    @Override
    public List<AsyncEventChannelMessageHandler<JsonNode>> provideForTarget(final Purpose purpose) {
        Objects.requireNonNull(purpose);
        return cache.computeIfAbsent(purpose, target1 ->
                eventChannelMessageHandlers.select(AsyncEventConsumerChannel.Literal.of(target1.name())).stream().toList());
    }
}
