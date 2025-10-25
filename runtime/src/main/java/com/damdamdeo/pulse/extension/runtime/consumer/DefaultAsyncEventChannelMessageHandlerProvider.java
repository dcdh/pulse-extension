package com.damdamdeo.pulse.extension.runtime.consumer;

import com.damdamdeo.pulse.extension.core.consumer.AsyncEventChannelMessageHandler;
import com.damdamdeo.pulse.extension.core.consumer.AsyncEventChannelMessageHandlerProvider;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class DefaultAsyncEventChannelMessageHandlerProvider implements AsyncEventChannelMessageHandlerProvider<JsonNode> {

    private final Instance<AsyncEventChannelMessageHandler<JsonNode>> eventChannelMessageHandlers;

    private final Map<Target, List<AsyncEventChannelMessageHandler<JsonNode>>> cache;

    public DefaultAsyncEventChannelMessageHandlerProvider(final Instance<AsyncEventChannelMessageHandler<JsonNode>> eventChannelMessageHandlers) {
        this.eventChannelMessageHandlers = Objects.requireNonNull(eventChannelMessageHandlers);
        this.cache = new HashMap<>();
    }

    @Override
    public List<AsyncEventChannelMessageHandler<JsonNode>> provideForTarget(final Target target) {
        Objects.requireNonNull(target);
        return cache.computeIfAbsent(target, target1 ->
                eventChannelMessageHandlers.select(EventChannel.Literal.of(target1.name())).stream().toList());
    }
}
